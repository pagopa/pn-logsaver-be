package it.pagopa.pn.logsaver.dao.dynamo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExtraType;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class StorageDaoDynamoImpl implements StorageDao {
  private final String TABLE = "audit_storage";
  private final String FIRST_START_DAY = "2022-07-10";

  private final DynamoDbEnhancedClient enhancedClient;



  @Override
  public List<AuditStorageEntity> getAudits(LocalDate dateFrom, LocalDate dateTo,
      List<Retention> retentions) {

    List<AuditStorageEntity> retList = new ArrayList<>();

    DynamoDbTable<AuditStorageEntity> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(AuditStorageEntity.class));

    Expression expression = Expression.builder().expression("#result = :result")
        .putExpressionValue(":result", AttributeValue.builder().s("SUCCESS").build())
        .putExpressionName("#result", "result").build();

    retentions.stream().forEach(retention -> findItemInPartion(dateFrom, dateTo, auditStorageTable,
        retention, retList, expression));

    return retList;
  }

  @Override
  public AuditStorageEntity getAudit(LocalDate dateLog, Retention retention) {
    DynamoDbTable<AuditStorageEntity> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(AuditStorageEntity.class));

    return auditStorageTable.getItem(Key.builder().partitionValue(retention.name())
        .sortValue(DateUtils.format(dateLog)).build());
  }


  @Override
  public void insertAudit(AuditStorageEntity as) {

    DynamoDbTable<AuditStorageEntity> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(AuditStorageEntity.class));

    auditStorageTable.putItem(as);
  }


  private void findItemInPartion(LocalDate dateFrom, LocalDate dateTo,
      DynamoDbTable<AuditStorageEntity> auditStorageTable, Retention retention,
      List<AuditStorageEntity> retList, Expression expression) {

    QueryConditional queryConditional = QueryConditional.sortBetween(
        Key.builder().partitionValue(retention.name()).sortValue(DateUtils.format(dateFrom))
            .build(),
        Key.builder().partitionValue(retention.name()).sortValue(DateUtils.format(dateTo)).build());

    auditStorageTable.query(r -> r.queryConditional(queryConditional).filterExpression(expression))
        .items().stream().collect(Collectors.toCollection(() -> retList));
  }


  @Override
  public ExecutionEntity latestExecution() {

    DynamoDbTable<ExecutionEntity> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(ExecutionEntity.class));

    QueryConditional queryConditional = QueryConditional
        .keyEqualTo(Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name()).build());

    return auditStorageTable
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional)
            .scanIndexForward(false).limit(Integer.valueOf(1)).build())
        .items().stream().findFirst().orElseGet(() -> {
          log.warn("No execution date in the table. Insert first execution with date {}",
              FIRST_START_DAY);
          return updateExecution(DateUtils.parse(FIRST_START_DAY), ItemType.valuesAsString(),
              ExportType.PDF);
        });
  }


  @Override
  public ExecutionEntity updateExecution(LocalDate day, List<String> types, ExportType typeExport) {

    DynamoDbTable<ExecutionEntity> auditStorageTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(ExecutionEntity.class));

    ExecutionEntity last = ExecutionEntity.builder().type(ExtraType.LOG_SAVER_EXECUTION.name())
        .logDate(DateUtils.format(day)).latestExecutionDate(day).typesProcessed(types)
        .exportType(typeExport.name()).build();

    auditStorageTable.updateItem(last);
    return last;

  }

}
