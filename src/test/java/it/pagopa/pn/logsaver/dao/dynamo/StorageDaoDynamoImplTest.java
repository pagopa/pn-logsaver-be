package it.pagopa.pn.logsaver.dao.dynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.TestUtils;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ContinuosExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.ExtraType;
import it.pagopa.pn.logsaver.dao.support.StorageDaoLogicSupport;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.LogFileType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.springbootcfg.AwsConfigs;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.Update;

@ExtendWith(MockitoExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class StorageDaoDynamoImplTest {

  private static DynamoDbEnhancedClient enhancedClient;
  private static DynamoDbTable<AuditStorageEntity> auditStorageTable;
  private static DynamoDbTable<ExecutionEntity> executionTable;
  private static DynamoDbTable<ContinuosExecutionEntity> continuosExecutionTable;

  private StorageDao storageDao;
  @Mock
  private AwsConfigs awsCfg;

  @Captor
  private ArgumentCaptor<TransactWriteItemsEnhancedRequest> transacRequest;

  @BeforeEach
  void init() {
    when(awsCfg.getDynamoDbTableName()).thenReturn("audit_storage");
    enhancedClient = Mockito.mock(DynamoDbEnhancedClient.class);
    auditStorageTable = Mockito.mock(DynamoDbTable.class);
    executionTable = Mockito.mock(DynamoDbTable.class);
    continuosExecutionTable = Mockito.mock(DynamoDbTable.class);
    when(enhancedClient.table(anyString(), any())).then(in -> {
      BeanTableSchema schema = in.getArgument(1);
      if (schema.itemType().rawClass().equals(AuditStorageEntity.class)) {
        return auditStorageTable;
      } else if (schema.itemType().rawClass().equals(ExecutionEntity.class)) {
        return executionTable;
      } else {
        return continuosExecutionTable;
      }
    });

    storageDao = new StorageDaoDynamoImpl(awsCfg, enhancedClient);
    Method init = ReflectionUtils.findMethod(StorageDaoDynamoImpl.class, "init");
    ReflectionUtils.makeAccessible(init);
    ReflectionUtils.invokeMethod(init, storageDao);

    verify(continuosExecutionTable, times(1)).putItem(any(PutItemEnhancedRequest.class));
    verify(executionTable, times(1)).putItem(any(PutItemEnhancedRequest.class));

  }

  @Test
  void latestExecution() {
    ExecutionEntity mock = ExecutionEntity.builder().logFileTypes(LogFileType.valuesAsString())
        .logDate(TestCostant.LOGDATE.toString())
        .retentionResult(StorageDaoLogicSupport.defaultResultMap()).build();
    when(executionTable.query(any(QueryEnhancedRequest.class)))
        .thenReturn(execListMock(List.of(mock)));
    ExecutionEntity exEnt = storageDao.getLatestExecution();
    assertTrue(TestUtils.equals(mock, exEnt));
    verify(executionTable, times(1)).query(any(QueryEnhancedRequest.class));
  }

  @Test
  void latestExecution_NotFound() {
    when(executionTable.query(any(QueryEnhancedRequest.class))).thenReturn(execListMock(List.of()));

    assertThrows(InternalException.class, () -> storageDao.getLatestExecution());
    verify(executionTable, times(1)).query(any(QueryEnhancedRequest.class));
  }

  private PageIterable<ExecutionEntity> execListMock(List<ExecutionEntity> mockList) {
    return new PageIterable<ExecutionEntity>() {
      @Override
      public Iterator<Page<ExecutionEntity>> iterator() {
        return new Iterator<Page<ExecutionEntity>>() {

          private Page<ExecutionEntity> page = Page.create(mockList);
          private Iterator iterable = page.items().iterator();
          int cnt = 0;

          @Override
          public boolean hasNext() {
            return cnt < mockList.size();
          }

          @Override
          public Page<ExecutionEntity> next() {
            return cnt++ < mockList.size() ? page : null;
          }
        };
      }

    };

  }



  @Test
  void latestContinuosExecution() {
    ContinuosExecutionEntity mock = new ContinuosExecutionEntity(TestCostant.LOGDATE);
    when(continuosExecutionTable.query(any(QueryEnhancedRequest.class)))
        .thenReturn(continuosExecListMock(List.of(mock)));

    LocalDate exEnt = storageDao.getLatestContinuosExecution();
    assertEquals(TestCostant.LOGDATE, exEnt);
    verify(continuosExecutionTable, times(1)).query(any(QueryEnhancedRequest.class));
  }

  @Test
  void latestContinuosExecution_NotFound() {
    when(continuosExecutionTable.query(any(QueryEnhancedRequest.class)))
        .thenReturn(continuosExecListMock(List.of()));

    assertThrows(InternalException.class, () -> storageDao.getLatestContinuosExecution());
    verify(continuosExecutionTable, times(1)).query(any(QueryEnhancedRequest.class));
  }

  private PageIterable<ContinuosExecutionEntity> continuosExecListMock(
      List<ContinuosExecutionEntity> mockList) {
    return new PageIterable<ContinuosExecutionEntity>() {
      @Override
      public Iterator<Page<ContinuosExecutionEntity>> iterator() {
        return new Iterator<Page<ContinuosExecutionEntity>>() {
          private Page<ContinuosExecutionEntity> page = Page.create(mockList);
          private Iterator iterable = page.items().iterator();
          int cnt = 0;

          @Override
          public boolean hasNext() {
            return cnt < mockList.size();
          }

          @Override
          public Page<ContinuosExecutionEntity> next() {
            return cnt++ < mockList.size() ? page : null;
          }
        };
      }
    };
  }

  @Test
  void executionBetween() {
    ExecutionEntity mock = ExecutionEntity.builder().logFileTypes(LogFileType.valuesAsString())
        .logDate(TestCostant.LOGDATE.toString())
        .retentionResult(StorageDaoLogicSupport.defaultResultMap()).build();

    when(executionTable.query(any(QueryEnhancedRequest.class)))
        .thenReturn(execListMock(List.of(mock)));

    List<ExecutionEntity> res =
        storageDao.getExecutionBetween(TestCostant.LOGDATE_FROM, TestCostant.LOGDATE);
    verify(executionTable, times(1)).query(any(QueryEnhancedRequest.class));
    assertTrue(TestUtils.equals(mock, res.get(0)));
  }


  @Test
  void updateExecution() {

    ExecutionEntity mock = ExecutionEntity.builder().logFileTypes(LogFileType.valuesAsString())
        .logDate("2022-07-13").retentionResult(StorageDaoLogicSupport.defaultResultMap()).build();

    ExecutionEntity mock2 = ExecutionEntity.builder().logFileTypes(LogFileType.valuesAsString())
        .logDate("2022-07-12").retentionResult(StorageDaoLogicSupport.defaultResultMap()).build();

    when(executionTable.query(any(QueryEnhancedRequest.class)))
        .thenReturn(execListMock(List.of(mock, mock2)));

    ContinuosExecutionEntity mockCont =
        new ContinuosExecutionEntity(TestCostant.LATEST_CONTUOS_EXEC_DATE);
    when(continuosExecutionTable.query(any(QueryEnhancedRequest.class)))
        .thenReturn(continuosExecListMock(List.of(mockCont)));

    when(auditStorageTable.tableSchema())
        .thenReturn(TableSchema.fromBean(AuditStorageEntity.class));
    when(executionTable.tableSchema()).thenReturn(TableSchema.fromBean(ExecutionEntity.class));
    when(continuosExecutionTable.tableSchema())
        .thenReturn(TableSchema.fromBean(ContinuosExecutionEntity.class));


    doNothing().when(enhancedClient).transactWriteItems(transacRequest.capture());


    List<AuditStorageEntity> auditFiles = List.of(
        AuditStorageEntity.builder().exportType(ExportType.PDF_SIGNED)
            .result(AuditStorageStatus.SENT.name()).logDate(TestCostant.LOGDATE.toString())
            .retention(Retention.AUDIT10Y).build(),
        AuditStorageEntity.builder().exportType(ExportType.PDF_SIGNED)
            .result(AuditStorageStatus.SENT.name()).logDate(TestCostant.LOGDATE.toString())
            .retention(Retention.AUDIT5Y).build(),
        AuditStorageEntity.builder().exportType(ExportType.PDF_SIGNED)
            .result(AuditStorageStatus.SENT.name()).logDate(TestCostant.LOGDATE.toString())
            .retention(Retention.DEVELOPER).build());
    storageDao.updateExecution(auditFiles, TestCostant.LOGDATE, Set.of(LogFileType.values()));

    TransactWriteItemsEnhancedRequest transac = transacRequest.getValue();

    List<Put> updContinuosExec = TestUtils.searchPut(transac.transactWriteItems(), "type",
        ExtraType.CONTINUOS_EXECUTION.name());

    List<Update> updLastExec = TestUtils.searchUpd(transac.transactWriteItems(), "type",
        ExtraType.LOG_SAVER_EXECUTION.name());

    List<Put> updAudit10Pdf = TestUtils.searchPut(transac.transactWriteItems(), "type",
        String.join("$", Retention.AUDIT10Y.name(), ExportType.PDF_SIGNED.name()));
    List<Put> updAudit5df = TestUtils.searchPut(transac.transactWriteItems(), "type",
        String.join("$", Retention.AUDIT5Y.name(), ExportType.PDF_SIGNED.name()));
    List<Put> updAuditDevPdf = TestUtils.searchPut(transac.transactWriteItems(), "type",
        String.join("$", Retention.DEVELOPER.name(), ExportType.PDF_SIGNED.name()));

    assertEquals(1, updContinuosExec.size());
    assertEquals(1, updLastExec.size());
    assertEquals(1, updAudit10Pdf.size());
    assertEquals(1, updAudit5df.size());
    assertEquals(1, updAuditDevPdf.size());
    // :AMZN_MAPPED_
    assertEquals("2022-07-13", updContinuosExec.get(0).item().get("latestExecutionDate").s());

  }
}
