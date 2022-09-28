package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.dao.support.StorageDaoLogicSupport;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.services.StorageService;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

  @Mock
  private PnSafeStorageClient safeStorageClient;
  @Mock
  private StorageDao storageDao;

  private StorageService service;


  @BeforeEach
  void setUp() {
    this.service = new StorageServiceImpl(safeStorageClient, storageDao);
  }

  @Test
  void store() {
    List<AuditFile> auditFiles = TestCostant.auditFiles;

    when(safeStorageClient.uploadFile(any())).thenAnswer(inTarg -> {
      AuditStorage audit = inTarg.getArgument(0, AuditStorage.class);
      return audit.uploadKey("KEY");
    });
    doNothing().when(storageDao).updateExecution(any(), any(), any());


    List<AuditStorage> auditStorageRes = service.store(auditFiles, TestCostant.CTX);

    verify(safeStorageClient, times(auditFiles.size())).uploadFile(any());
    verify(storageDao, times(1)).updateExecution(any(), any(), any());
    assertNotNull(auditStorageRes);
    assertEquals(3, auditStorageRes.size());
    auditStorageRes.forEach(aud -> {
      assertEquals("KEY", aud.uploadKey());
    });
  }

  @Test
  void latestStorageExecution() {
    Map<String, RetentionResult> retentionResult = StorageDaoLogicSupport.defaultResultMap();

    when(storageDao.latestExecution())
        .thenReturn(ExecutionEntity.builder().itemTypes(ItemType.valuesAsString())
            .retentionResult(retentionResult).logDate(TestCostant.LOGDATE.toString()).build());

    StorageExecution latest = service.latestStorageExecution();

    verify(storageDao, times(1)).latestExecution();
    assertNotNull(latest);
    assertEquals(TestCostant.LOGDATE, latest.getLogDate());
    assertEquals(Set.of(ItemType.values()), latest.getItemTypes());
    assertEquals(retentionResult.values().size(), latest.getDetails().size());
  }

  @Test
  void latestContinuosExecutionDate() {

    when(storageDao.latestContinuosExecution()).thenReturn(TestCostant.LOGDATE);

    LocalDate latestConDate = service.latestContinuosExecutionDate();

    verify(storageDao, times(1)).latestContinuosExecution();
    assertNotNull(latestConDate);
    assertEquals(TestCostant.LOGDATE, latestConDate);
  }

  @Test
  void storageExecutionBetween() {

    when(storageDao.executionBetween(any(), any()))
        .thenReturn(List.of(ExecutionEntity.builder().itemTypes(ItemType.valuesAsString())
            .retentionResult(StorageDaoLogicSupport.defaultResultMap())
            .logDate(TestCostant.LOGDATE.toString()).build()));
    List<StorageExecution> resList =
        service.storageExecutionBetween(TestCostant.LOGDATE_FROM, TestCostant.LOGDATE);

    verify(storageDao, times(1)).executionBetween(any(), any());

    assertNotNull(resList);
    assertEquals(1, resList.size());
  }

}
