package it.pagopa.pn.logsaver.dao.dynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import it.pagopa.pn.logsaver.config.ClApplicationArguments;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ContinuosExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.services.TTLService;
import it.pagopa.pn.logsaver.springbootcfg.AwsConfigs;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@ExtendWith(MockitoExtension.class)
class StorageDaoDynamoImplFirstStartDayTest {

  private static final LocalDate CONFIGURED_FIRST_START_DAY = LocalDate.parse("2024-05-10");

  private DynamoDbTable<ExecutionEntity> executionTable;
  private DynamoDbTable<ContinuosExecutionEntity> continuosExecutionTable;
  private StorageDaoDynamoImpl storageDao;

  @Mock
  private AwsConfigs awsCfg;
  @Mock
  private ClApplicationArguments args;
  @Mock
  private LogSaverCfg logSaverCfg;
  @Mock
  private TTLService ttlService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    when(awsCfg.getDynamoDbTableName()).thenReturn("audit_storage");
    lenient().when(logSaverCfg.getFirstStartDay()).thenReturn(CONFIGURED_FIRST_START_DAY);
    DynamoDbEnhancedClient enhancedClient = mock(DynamoDbEnhancedClient.class);
    DynamoDbTable<AuditStorageEntity> auditStorageTable = mock(DynamoDbTable.class);
    executionTable = mock(DynamoDbTable.class);
    continuosExecutionTable = mock(DynamoDbTable.class);
    when(enhancedClient.table(anyString(), any())).then(in -> {
      BeanTableSchema<?> schema = in.getArgument(1);
      if (schema.itemType().rawClass().equals(AuditStorageEntity.class)) {
        return auditStorageTable;
      } else if (schema.itemType().rawClass().equals(ExecutionEntity.class)) {
        return executionTable;
      }
      return continuosExecutionTable;
    });
    storageDao = new StorageDaoDynamoImpl(awsCfg, args, logSaverCfg, enhancedClient, ttlService);
  }

  private void invokeInit() {
    Method init = ReflectionUtils.findMethod(StorageDaoDynamoImpl.class, "init");
    ReflectionUtils.makeAccessible(init);
    ReflectionUtils.invokeMethod(init, storageDao);
  }

  @Test
  @SuppressWarnings("unchecked")
  void init_recordMissing_insertsAnchorThenContinuosOnTheDayBeforeConfiguredFirstStartDay() {
    invokeInit();

    InOrder order = inOrder(executionTable, continuosExecutionTable);
    ArgumentCaptor<PutItemEnhancedRequest<ExecutionEntity>> anchorPut =
        ArgumentCaptor.forClass(PutItemEnhancedRequest.class);
    order.verify(executionTable, times(1)).putItem(anchorPut.capture());
    ArgumentCaptor<PutItemEnhancedRequest<ContinuosExecutionEntity>> continuosPut =
        ArgumentCaptor.forClass(PutItemEnhancedRequest.class);
    order.verify(continuosExecutionTable, times(1)).putItem(continuosPut.capture());

    assertEquals("2024-05-09", anchorPut.getValue().item().getLogDate());
    ContinuosExecutionEntity continuos = continuosPut.getValue().item();
    assertEquals(LocalDate.parse("2024-05-09"), continuos.getLatestExecutionDate());
    assertEquals("2023-03-01", continuos.getLogDate());
  }

  @Test
  @SuppressWarnings("unchecked")
  void init_recordMissingWithDefaultFirstStartDay_writesSameRowsAsHardcodedDate() {
    when(logSaverCfg.getFirstStartDay()).thenReturn(LocalDate.parse("2023-03-02"));

    invokeInit();

    ArgumentCaptor<PutItemEnhancedRequest<ExecutionEntity>> anchorPut =
        ArgumentCaptor.forClass(PutItemEnhancedRequest.class);
    verify(executionTable, times(1)).putItem(anchorPut.capture());
    ArgumentCaptor<PutItemEnhancedRequest<ContinuosExecutionEntity>> continuosPut =
        ArgumentCaptor.forClass(PutItemEnhancedRequest.class);
    verify(continuosExecutionTable, times(1)).putItem(continuosPut.capture());

    assertEquals("2023-03-01", anchorPut.getValue().item().getLogDate());
    assertEquals(LocalDate.parse("2023-03-01"), continuosPut.getValue().item().getLatestExecutionDate());
    assertEquals("2023-03-01", continuosPut.getValue().item().getLogDate());
  }

  @Test
  void init_recordPresent_doesNotWriteAnything() {
    when(continuosExecutionTable.getItem(any(Key.class)))
        .thenReturn(new ContinuosExecutionEntity(LocalDate.parse("2026-10-01")));

    invokeInit();

    verify(continuosExecutionTable, never()).putItem(any(PutItemEnhancedRequest.class));
    verify(executionTable, never()).putItem(any(PutItemEnhancedRequest.class));
  }

  @Test
  void init_recordMissingAndAnchorAlreadyPresent_insertsContinuos() {
    doThrow(ConditionalCheckFailedException.builder().message("exists").build())
        .when(executionTable).putItem(any(PutItemEnhancedRequest.class));

    invokeInit();

    verify(continuosExecutionTable, times(1)).putItem(any(PutItemEnhancedRequest.class));
  }
}
