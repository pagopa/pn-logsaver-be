package it.pagopa.pn.logsaver.dao.dynamo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorage;
import it.pagopa.pn.logsaver.dao.entity.ExtraType;
import it.pagopa.pn.logsaver.dao.entity.Execution;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Component
@AllArgsConstructor
public class StorageDaoDynamoImpl implements StorageDao {
  private final String TABLE = "audit_storage";
  private final String FIRST_START_DAY = "2022-07-10";

  private final DynamoDbEnhancedClient enhancedClient;



  @Override
  public List<AuditStorage> getAudits(LocalDate dateFrom, LocalDate dateTo,
      List<Retention> retentions) {

    List<AuditStorage> retList = new ArrayList<>();

    DynamoDbTable<AuditStorage> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(AuditStorage.class));

    Expression expression = Expression.builder().expression("#result = :result")
        .putExpressionValue(":result", AttributeValue.builder().s("SUCCESS").build())
        .putExpressionName("#result", "result").build();

    retentions.stream().forEach(retention -> findItemInPartion(dateFrom, dateTo, auditStorageTable,
        retention, retList, expression));

    return retList;


  }

  @Override
  public void insertAudit(AuditStorage as) {

    DynamoDbTable<AuditStorage> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(AuditStorage.class));

    auditStorageTable.putItem(as);
  }


  private void findItemInPartion(LocalDate dateFrom, LocalDate dateTo,
      DynamoDbTable<AuditStorage> auditStorageTable, Retention retention,
      List<AuditStorage> retList, Expression expression) {


    QueryConditional queryConditional = QueryConditional.sortBetween(
        Key.builder().partitionValue(retention.name()).sortValue(DateUtils.format(dateFrom))
            .build(),
        Key.builder().partitionValue(retention.name()).sortValue(DateUtils.format(dateTo)).build());

    Iterator<AuditStorage> results = auditStorageTable
        .query(r -> r.queryConditional(queryConditional).filterExpression(expression)).items()
        .iterator();

    while (results.hasNext()) {
      AuditStorage rec = results.next();
      retList.add(rec);
    }

  }


  @Override
  public Execution latestExecution() {

    DynamoDbTable<Execution> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(Execution.class));

    Iterator<Execution> results = auditStorageTable.query(QueryEnhancedRequest
        .builder()
        .queryConditional(QueryConditional
            .keyEqualTo(Key.builder().partitionValue(ExtraType.LATEST_EXECUTION.name()).build()))
        .scanIndexForward(false).limit(Integer.valueOf(1)).build()).items().iterator();
    if (results.hasNext()) {
      return results.next();
    }
    return updateLatestExecution(DateUtils.parse(FIRST_START_DAY), ItemType.valuesAsString());
  }


  @Override
  public Execution updateLatestExecution(LocalDate day, List<String> types) {

    DynamoDbTable<Execution> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(Execution.class));

    Execution last =
        Execution.builder().type(ExtraType.LATEST_EXECUTION.name())
            .logDate(FIRST_START_DAY).latestExecutionDate(day).typesProcessed(types).build();

    auditStorageTable.updateItem(last);
    return last;

  }


}
