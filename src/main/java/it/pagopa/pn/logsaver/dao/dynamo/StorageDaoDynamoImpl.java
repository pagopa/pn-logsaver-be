package it.pagopa.pn.logsaver.dao.dynamo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.dao.AuditStorageMapper;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ContinuosExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExtraType;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.dao.support.StorageDaoLogicSupport;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
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
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

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
    insertFirstContinuosExecution();
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
        .items().stream().findFirst()
        .orElseThrow(() -> new InternalException("No execution date in the table."));
  }



  private ExecutionEntity insertFirtsExecution() {
    // Dettaglio esecuzione di default
    Map<String, RetentionResult> def = StorageDaoLogicSupport.defaultResultMap();

    ExecutionEntity last = ExecutionEntity.builder().logDate(FIRST_START_DAY)
        .itemTypes(ItemType.valuesAsString()).retentionResult(def).build();

    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":execution",
        AttributeValue.builder().s(ExtraType.LOG_SAVER_EXECUTION.name()).build());

    Map<String, String> expressionNames = new HashMap<>();
    expressionNames.put("#type", "type");

    PutItemEnhancedRequest<ExecutionEntity> insert = PutItemEnhancedRequest
        .builder(ExecutionEntity.class)
        .conditionExpression(Expression.builder().expression(" #type <> :execution ")
            .expressionValues(expressionValues).expressionNames(Map.of("#type", "type")).build())
        .item(last).build();
    try {
      executionTable.putItem(insert);
      log.info("Insert first execution with date {}", FIRST_START_DAY);
    } catch (ConditionalCheckFailedException e) {
      log.info("Execution date in the table. ");
    }
    return last;
  }

  @Override
  public LocalDate latestContinuosExecution() {

    QueryConditional queryConditional = QueryConditional
        .keyEqualTo(Key.builder().partitionValue(ExtraType.CONTINUOS_EXECUTION.name()).build());

    return continuosExecutionTable
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional)
            .scanIndexForward(false).limit(Integer.valueOf(1)).build())
        .items().stream().findFirst()
        .orElseThrow(() -> new InternalException("No continuos execution date in the table."))
        .getLatestExecutionDate();

  }



  private void insertFirstContinuosExecution() {

    PutItemEnhancedRequest<ContinuosExecutionEntity> insert =
        PutItemEnhancedRequest.builder(ContinuosExecutionEntity.class)
            .conditionExpression(Expression.builder()
                .expression("attribute_not_exists( "
                    .concat(ContinuosExecutionEntity.FIELD_EXECUTION_DATE).concat(" )"))
                .build())
            .item(new ContinuosExecutionEntity(DateUtils.parse(FIRST_START_DAY))).build();
    try {
      continuosExecutionTable.putItem(insert);
      log.info("Insert first continuos execution with date {}", FIRST_START_DAY);
    } catch (ConditionalCheckFailedException e) {
      log.info("Continuos execution date in the table.");
    }
  }


  @Override
  public void updateExecution(List<AuditStorageEntity> auditList, LocalDate logDate,
      Set<ItemType> types) {

    ExecutionEntity newExecution = ExecutionEntity.builder().logDate(DateUtils.format(logDate))
        .retentionResult(AuditStorageMapper.toResultExecution(auditList))
        .itemTypes(ItemType.valuesAsString(types)).build();

    TransactUpdateItemEnhancedRequest<ExecutionEntity> executionUpdate =
        TransactUpdateItemEnhancedRequest.builder(ExecutionEntity.class).item(newExecution).build();

    final TransactWriteItemsEnhancedRequest.Builder transBuild =
        TransactWriteItemsEnhancedRequest.builder().addUpdateItem(executionTable, executionUpdate);


    LocalDate lastContinuosExecutionReg = latestContinuosExecution();
    // Aggiorno La data ultima esecuzione continua se:
    // Tutti i file sono stati inviati
    // se la differenza tra la logDate e la data ultima esecuzione continua è 1
    if (!StorageDaoLogicSupport.hasErrors(newExecution) && Duration
        .between(lastContinuosExecutionReg.atStartOfDay(), logDate.atStartOfDay()).toDays() == 1) {

      // Determino la data esecuzione continua
      List<ExecutionEntity> execList = this.executionFrom(logDate);
      LocalDate lastContinuosExecutionDate =
          StorageDaoLogicSupport.computeLastContinuosExecutionDate(logDate, execList);

      TransactPutItemEnhancedRequest<ContinuosExecutionEntity> condtionalUpdate =
          TransactPutItemEnhancedRequest.builder(ContinuosExecutionEntity.class)
              .item(new ContinuosExecutionEntity(lastContinuosExecutionDate)).build();

      transBuild.addPutItem(continuosExecutionTable, condtionalUpdate);
    }

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
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional).build()).items()
        .stream().collect(Collectors.toList());
  }


  public List<ExecutionEntity> executionFrom(LocalDate dateFrom) {


    QueryConditional queryConditional = QueryConditional
        .sortGreaterThan(Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name())
            .sortValue(DateUtils.format(dateFrom)).build());
    return executionTable.query(QueryEnhancedRequest.builder().queryConditional(queryConditional)

        .build()).items().stream().collect(Collectors.toList());
  }
}
