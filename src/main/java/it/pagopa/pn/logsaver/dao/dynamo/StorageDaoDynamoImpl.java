package it.pagopa.pn.logsaver.dao.dynamo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorage;
import it.pagopa.pn.logsaver.dao.entity.ExtraType;
import it.pagopa.pn.logsaver.dao.entity.LatestSuccessStorage;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Component
@AllArgsConstructor
public class StorageDaoDynamoImpl implements StorageDao {
  private final String TABLE = "audit_storage";
  private final String FIRST_START_DAY = "2022-07-10";

  private final DynamoDbEnhancedClient enhancedClient;



  @Override
  public List<AuditStorage> getItems(LocalDate dateFrom, LocalDate dateTo,
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
  public LatestSuccessStorage latestSuccess() {

    DynamoDbTable<LatestSuccessStorage> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(LatestSuccessStorage.class));

    LatestSuccessStorage last = auditStorageTable.getItem(Key.builder()
        .partitionValue(ExtraType.LATEST_SUCCESS.name()).sortValue(FIRST_START_DAY).build());

    if (Objects.isNull(last)) {
      last = insertLatestSuccess(auditStorageTable, DateUtils.parse(FIRST_START_DAY),
          ItemType.valuesAsString());
    }

    return last;
  }



  @Override
  public LatestSuccessStorage updateLatestSuccess(LocalDate latestSuccessDay, List<String> types) {

    DynamoDbTable<LatestSuccessStorage> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(LatestSuccessStorage.class));

    LatestSuccessStorage last = LatestSuccessStorage.builder().type(ExtraType.LATEST_SUCCESS.name())
        .logDate(FIRST_START_DAY).latestSuccessDate(latestSuccessDay).typesProcessed(types).build();

    auditStorageTable.updateItem(last);
    return last;

  }


  // public LatestSuccessStorage insertLatestSuccess(LocalDate latestSuccessDay, List<String> types)
  // {
  //
  // try {
  //
  // DynamoDbTable<LatestSuccessStorage> auditStorageTable =
  // enhancedClient.table(TABLE, TableSchema.fromBean(LatestSuccessStorage.class));
  //
  // LatestSuccessStorage last = insertLatestSuccess(auditStorageTable, latestSuccessDay, types);
  //
  //
  // return last;
  //
  // } catch (DynamoDbException e) {
  // System.err.println(e.getMessage());
  // throw e;
  // }
  // }


  private LatestSuccessStorage insertLatestSuccess(
      DynamoDbTable<LatestSuccessStorage> auditStorageTable, LocalDate latestSuccessDay,
      List<String> types) {

    LatestSuccessStorage last = LatestSuccessStorage.builder().type(ExtraType.LATEST_SUCCESS.name())
        .logDate(FIRST_START_DAY).latestSuccessDate(latestSuccessDay).typesProcessed(types).build();

    auditStorageTable.putItem(last);

    return last;

  }

}
