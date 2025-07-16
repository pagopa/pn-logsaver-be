package it.pagopa.pn.logsaver.dao.dynamo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;

import it.pagopa.pn.logsaver.services.TTLService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.dao.AuditStorageMapper;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ContinuosExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExtraType;
import it.pagopa.pn.logsaver.dao.support.StorageDaoLogicSupport;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.springbootcfg.AwsConfigs;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "aws.use-dynamoDb", havingValue = "true", matchIfMissing = true)
public class StorageDaoDynamoImpl implements StorageDao {

  @NonNull
  private final AwsConfigs cfg;
  @NonNull
  private final ClApplicationArguments args;
  @NonNull
  private final DynamoDbEnhancedClient enhancedClient;
  private DynamoDbTable<AuditStorageEntity> auditStorageTable;
  private DynamoDbTable<ExecutionEntity> executionTable;
  private DynamoDbTable<ContinuosExecutionEntity> continuosExecutionTable;

  private final TTLService ttlService;

  @PostConstruct
  void init() {
    auditStorageTable = enhancedClient.table(cfg.getDynamoDbTableName(),
        TableSchema.fromBean(AuditStorageEntity.class));
    executionTable = enhancedClient.table(cfg.getDynamoDbTableName(),
        TableSchema.fromBean(ExecutionEntity.class));
    continuosExecutionTable = enhancedClient.table(cfg.getDynamoDbTableName(),
        TableSchema.fromBean(ContinuosExecutionEntity.class));
    insertFirstContinuosExecution();
    insertFirtsExecution();
  }


  @Override
  public ExecutionEntity getLatestExecution() {

    QueryConditional queryConditional = QueryConditional
        .keyEqualTo(Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name()).build());

    return executionTable
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional)
            .scanIndexForward(false).limit(Integer.valueOf(1)).build())
        .items().stream().findFirst()
        .orElseThrow(() -> new InternalException("No execution date in the table."));
  }



  private ExecutionEntity insertFirtsExecution() {
    ExecutionEntity first = StorageDaoLogicSupport.firstExececutionRow();
    if (Objects.nonNull(args.getRetentionExportTypesMap())) {
      first.setRetentionResult(AuditStorageMapper.toEntity(args.getRetentionExportTypesMap()));
    }

    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":execution",
        AttributeValue.builder().s(ExtraType.LOG_SAVER_EXECUTION.name()).build());

    Map<String, String> expressionNames = new HashMap<>();
    expressionNames.put("#type", "type");

    PutItemEnhancedRequest<ExecutionEntity> insert = PutItemEnhancedRequest
        .builder(ExecutionEntity.class)
        .conditionExpression(Expression.builder().expression(" #type <> :execution ")
            .expressionValues(expressionValues).expressionNames(Map.of("#type", "type")).build())
        .item(first).build();
    try {
      executionTable.putItem(insert);
      log.info("Insert first execution with date {}", FIRST_START_DAY);
    } catch (ConditionalCheckFailedException e) {
      log.info("Execution date in the table. ");
    }
    return first;
  }

  @Override
  public LocalDate getLatestContinuosExecution() {

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
  public void updateExecution(List<AuditStorageEntity> auditList, LocalDate logDate, Set<LogFileType> types, Boolean continuousExecutionUpdate) {

    // Se si ha la necesssità di aaumentare il numero di righe per transazione, monitorare eventuali
    // limiti

    Duration offsetDuration = ttlService.getOffsetDuration();
    // Riga dettaglio esecuzione
    ExecutionEntity newExecution = StorageDaoLogicSupport.from(auditList, logDate, types, offsetDuration);
    log.info("New execution for date {} - Offset: {} - entity: {}", logDate, offsetDuration, newExecution);

    ExecutionEntity oldExecution = getExecution(logDate);
    if (Objects.nonNull(oldExecution)) {
      newExecution.setRetentionResult(StorageDaoLogicSupport.mergeRetentionResult(
          oldExecution.getRetentionResult(), newExecution.getRetentionResult()));
    }
    TransactUpdateItemEnhancedRequest<ExecutionEntity> executionUpdate =
        TransactUpdateItemEnhancedRequest.builder(ExecutionEntity.class).item(newExecution).build();
    final TransactWriteItemsEnhancedRequest.Builder transBuild =
        TransactWriteItemsEnhancedRequest.builder().addUpdateItem(executionTable, executionUpdate);


    LocalDate lastContinuosExecutionReg = getLatestContinuosExecution();
    // Aggiorno La data ultima esecuzione continua se:
    // Tutti i file sono stati inviati
    // se la differenza tra la logDate e la data ultima esecuzione continua è 1
    if (!StorageDaoLogicSupport.hasErrors(newExecution) && Duration
        .between(lastContinuosExecutionReg.atStartOfDay(), logDate.atStartOfDay()).toDays() == 1 && continuousExecutionUpdate) {

      // Determino la data esecuzione continua
      List<ExecutionEntity> execList = this.executionFrom(logDate);
      LocalDate lastContinuosExecutionDate =
          StorageDaoLogicSupport.computeLastContinuosExecutionDate(logDate, execList);

      TransactPutItemEnhancedRequest<ContinuosExecutionEntity> condtionalUpdate =
          TransactPutItemEnhancedRequest.builder(ContinuosExecutionEntity.class)
              .item(new ContinuosExecutionEntity(lastContinuosExecutionDate)).build();
      // Riga ultima esecuzioe consecutiva
      transBuild.addPutItem(continuosExecutionTable, condtionalUpdate);
    }
    // Una riga per ogni file generato
    auditList.stream().forEach(entity -> transBuild.addPutItem(auditStorageTable, entity));

    enhancedClient.transactWriteItems(transBuild.build());

  }

  private ExecutionEntity getExecution(LocalDate logDate) {

    return executionTable.getItem(Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name())
        .sortValue(logDate.toString()).build());
  }



  @Override
  public List<ExecutionEntity> getExecutionBetween(LocalDate dateFrom, LocalDate dateTo) {


    QueryConditional queryConditional = QueryConditional.sortBetween(
        Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name())
            .sortValue(DateUtils.format(dateFrom)).build(),
        Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name())
            .sortValue(DateUtils.format(dateTo)).build());
    return executionTable
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional).build()).items()
        .stream().collect(Collectors.toList());
  }


  private List<ExecutionEntity> executionFrom(LocalDate dateFrom) {


    QueryConditional queryConditional = QueryConditional
        .sortGreaterThan(Key.builder().partitionValue(ExtraType.LOG_SAVER_EXECUTION.name())
            .sortValue(DateUtils.format(dateFrom)).build());
    return executionTable
        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional).build()).items()
        .stream().distinct().collect(Collectors.toList());
  }


  @Override
  public Stream<AuditStorageEntity> getAudits(String key, LocalDate dateFrom, LocalDate dateTo) {

    QueryConditional queryConditional = QueryConditional.sortBetween(
        Key.builder().partitionValue(key).sortValue(DateUtils.format(dateFrom)).build(),
        Key.builder().partitionValue(key).sortValue(DateUtils.format(dateTo)).build());

    return auditStorageTable.query(queryConditional).items().stream();

  }

  /**
   * Trova tutti i record con uno specifico risultato
   * @param result il risultato da cercare
   * @return Lista di AuditStorageEntity con il risultato specificato
   */
  //TODO: aggiungere il controllo sulla partition
  public List<AuditStorageEntity> findByResult(String result) {
    Expression expression = Expression.builder()
            .expression("#result = :result")
            .expressionNames(Map.of("#result", "result"))
            .expressionValues(Map.of(":result", AttributeValue.builder().s(result).build()))
            .build();

    ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
            .filterExpression(expression)
            .build();

    return auditStorageTable.scan(scanRequest)
            .items()
            .stream()
            .collect(Collectors.toList());
  }

  @Override
  public Stream<AuditStorageEntity> getAuditsByResult(String key, String result) {

    QueryConditional partitionKeyCondition = QueryConditional
            .keyEqualTo(Key.builder().partitionValue(key).build());

    Expression expression = Expression.builder()
            .expression("#result = :result")
            .expressionNames(Map.of("#result", "result"))
            .expressionValues(Map.of(":result", AttributeValue.builder().s(result).build()))
            .build();

    QueryEnhancedRequest qeRequest = QueryEnhancedRequest
            .builder()
            .queryConditional(partitionKeyCondition)
            .filterExpression(expression)
            //.scanIndexForward(true)
            .build();

    return auditStorageTable.query(qeRequest).items().stream();
  }

}
