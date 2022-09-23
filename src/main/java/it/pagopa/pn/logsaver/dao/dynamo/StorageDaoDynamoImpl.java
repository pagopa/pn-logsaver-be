package it.pagopa.pn.logsaver.dao.dynamo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.dao.AuditStorageMapper;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ContinuosExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExtraType;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageDaoDynamoImpl implements StorageDao {
  private static final String TABLE = "audit_storage";

  @NonNull
  private final DynamoDbEnhancedClient enhancedClient;
  private DynamoDbTable<AuditStorageEntity> auditStorageTable;
  private DynamoDbTable<ExecutionEntity> executionTable;
  private DynamoDbTable<ContinuosExecutionEntity> continuosExecutionTable;

  @PostConstruct
  void init() {
    auditStorageTable = enhancedClient.table(TABLE, TableSchema.fromBean(AuditStorageEntity.class));
    executionTable = enhancedClient.table(TABLE, TableSchema.fromBean(ExecutionEntity.class));
    continuosExecutionTable =
        enhancedClient.table(TABLE, TableSchema.fromBean(ContinuosExecutionEntity.class));
    insertFirtsExecution();
  }

  @Override
  public List<AuditStorageEntity> getAudits(LocalDate dateFrom, LocalDate dateTo,
      Set<Retention> retentions) {

    List<AuditStorageEntity> retList = new ArrayList<>();


    Expression expression = Expression.builder().expression("#result = :result")
        .putExpressionValue(":result",
            AttributeValue.builder().s(AuditStorageStatus.SENT.name()).build())
        .putExpressionName("#result", "result").build();

    retentions.stream()
        .forEach(retention -> findItemInPartion(dateFrom, dateTo, retention, retList, expression));

    return retList;
  }

  @Override
  public AuditStorageEntity getAudit(LocalDate dateLog, Retention retention) {


    return auditStorageTable.getItem(Key.builder().partitionValue(retention.name())
        .sortValue(DateUtils.format(dateLog)).build());
  }


  @Override
  public void insertAudit(AuditStorageEntity as) {

    auditStorageTable.putItem(as);
  }


  private void findItemInPartion(LocalDate dateFrom, LocalDate dateTo, Retention retention,
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

    QueryConditional queryConditional = QueryConditional
        .keyEqualTo(Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name()).build());

    return executionTable
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional)
            .scanIndexForward(false).limit(Integer.valueOf(1)).build())
        .items().stream().findFirst().orElseGet(() -> {
          log.warn("No execution date in the table. Insert first execution with date {}",
              FIRST_START_DAY);
          return insertFirtsExecution();
        });
  }



  private ExecutionEntity insertFirtsExecution() {
    Map<String, RetentionResult> def = Stream.of(Retention.values())
        .flatMap(ret -> Stream.of(ExportType.values()).map(
            expTy -> new RetentionResult(ret.name(), AuditStorageStatus.SENT.name(), expTy.name())))
        .collect(Collectors.toMap(RetentionResult::getKey, Function.identity()));

    ExecutionEntity last =
        ExecutionEntity.builder().logDate(FIRST_START_DAY).itemTypes(ItemType.valuesAsString())
            .exportTypes(ExportType.valuesAsString()).retentionResult(def).build();

    executionTable.updateItem(last);
    return last;
  }

  @Override
  public LocalDate latestContinuosExecution() {

    QueryConditional queryConditional = QueryConditional
        .keyEqualTo(Key.builder().partitionValue(ExtraType.CONTINUOS_EXECUTION.name()).build());

    return continuosExecutionTable
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional)
            .scanIndexForward(false).limit(Integer.valueOf(1)).build())
        .items().stream().findFirst().orElseGet(() -> {
          log.warn("No execution date in the table. Insert first continuos execution with date {}",
              FIRST_START_DAY);
          return insertFirstContinuosExecution();
        }).getLatestExecutionDate();
  }



  private ContinuosExecutionEntity insertFirstContinuosExecution() {
    return continuosExecutionTable
        .updateItem(new ContinuosExecutionEntity(DateUtils.parse(FIRST_START_DAY)));
  }

  @Override
  public void updateExecution(List<AuditStorageEntity> auditList, LocalDate day,
      Set<ItemType> types, Set<ExportType> typeExport, Set<Retention> retentions) {

    ExecutionEntity last = ExecutionEntity.builder().logDate(DateUtils.format(day))
        .retentionResult(AuditStorageMapper.toResultExecution(auditList))
        .itemTypes(ItemType.valuesAsString(types))
        .exportTypes(ExportType.valuesAsString(typeExport))
        .retentions(Retention.valuesAsString(retentions)).build();

    TransactWriteItemsEnhancedRequest.Builder transBuild =
        TransactWriteItemsEnhancedRequest.builder().addPutItem(executionTable, last);

    String previousDay = day.minusDays(1).toString();
    // Aggiorno La data ultima esecuzione continua se l'ultima data è uguale previousDay
    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":previousDay", AttributeValue.builder().s(previousDay).build());

    TransactPutItemEnhancedRequest<ContinuosExecutionEntity> condtionalUpdate =
        TransactPutItemEnhancedRequest.builder(ContinuosExecutionEntity.class)
            .item(new ContinuosExecutionEntity(day))
            .conditionExpression(Expression.builder()
                .expression(ContinuosExecutionEntity.FIELD_EXECUTION_DATE.concat(" = :previousDay"))
                .expressionValues(expressionValues).build())
            .build();

    // LocalDate lastContinuosExecDate = latestContinuosExecution();
    // if (Duration.between(lastContinuosExecDate, day).toDays() == 1) {
    transBuild.addPutItem(continuosExecutionTable, condtionalUpdate);
    // }

    auditList.stream().forEach(entity -> transBuild.addPutItem(auditStorageTable, entity));

    enhancedClient.transactWriteItems(transBuild.build());

  }

  @Override
  public List<ExecutionEntity> executionBetween(LocalDate dateFrom, LocalDate dateTo) {


    QueryConditional queryConditional = QueryConditional.sortBetween(
        Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name())
            .sortValue(DateUtils.format(dateFrom)).build(),
        Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name())
            .sortValue(DateUtils.format(dateTo)).build());
    return executionTable
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional)
            .scanIndexForward(false).limit(Integer.valueOf(1)).build())
        .items().stream().collect(Collectors.toList());
  }

}
