package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.dao.entity.ExecutionEntity;
import it.pagopa.pn.logsaver.dao.entity.RetentionResult;
import it.pagopa.pn.logsaver.dao.support.StorageDaoLogicSupport;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.AuditStorage.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.DailyAuditDownloadable;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
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
  void getAuditFile() {

    Map<String, RetentionResult> retentionResult = StorageDaoLogicSupport.defaultResultMap();
    List<ExecutionEntity> execList =
        List.of(ExecutionEntity.builder().logFileTypes(LogFileType.valuesAsString())
            .retentionResult(retentionResult).logDate(TestCostant.LOGDATE.toString()).build());
    when(storageDao.getExecutionBetween(any(), any())).thenReturn(execList);

    List<AuditStorageEntity> auditFiles = TestCostant.auditFilesEntity;
    when(storageDao.getAudits(any(), any(), any())).thenAnswer(ans -> auditFiles.stream());

    when(safeStorageClient.dowloadFileInfo(any())).thenAnswer(inTarg -> {
      AuditDownloadReference audit = inTarg.getArgument(0, AuditDownloadReference.class);
      return audit.downloadUrl("http://downloadurl");
    });


    List<DailyAuditDownloadable> auditStorageRes =
        service.getAuditFile(TestCostant.LOGDATE, TestCostant.LOGDATE);


    verify(storageDao, times(1)).getExecutionBetween(any(), any());
    verify(storageDao, times(6)).getAudits(any(), any(), any());
    verify(safeStorageClient, times(auditFiles.size() * 6)).dowloadFileInfo(any());

    assertNotNull(auditStorageRes);
    assertEquals(1, auditStorageRes.size());
    DailyAuditDownloadable dailyRes = auditStorageRes.get(0);
    assertEquals(auditFiles.size() * 6, dailyRes.audits().size());

    dailyRes.audits().forEach(aud -> {
      assertEquals("http://downloadurl", aud.downloadUrl());
    });
  }


  @Test
  void dowloadAuditFile() throws IOException {
    AuditDownloadReference file = AuditDownloadReference.builder().exportType(ExportType.PDF_SIGNED)
        .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).status(AuditStorageStatus.SENT)
        .uploadKey("updKey").build();

    when(safeStorageClient.dowloadFile(any(), any())).thenAnswer(inTarg -> {
      AuditDownloadReference audit = inTarg.getArgument(0, AuditDownloadReference.class);
      return audit.content(IOUtils.toInputStream("test", Charset.defaultCharset()));
    });

    AuditDownloadReference auditStorageRes =
        service.dowloadAuditFile(file, UnaryOperator.identity());
    verify(safeStorageClient, times(1)).dowloadFile(any(), any());
    assertEquals("test", IOUtils.toString(auditStorageRes.content(), Charset.defaultCharset()));

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

    when(storageDao.getLatestExecution())
        .thenReturn(ExecutionEntity.builder().logFileTypes(LogFileType.valuesAsString())
            .retentionResult(retentionResult).logDate(TestCostant.LOGDATE.toString()).build());

    StorageExecution latest = service.getLatestStorageExecution();

    verify(storageDao, times(1)).getLatestExecution();
    assertNotNull(latest);
    assertEquals(TestCostant.LOGDATE, latest.getLogDate());
    assertEquals(Set.of(LogFileType.values()), latest.getLogFileTypes());
    assertEquals(retentionResult.values().size(), latest.getDetails().size());
  }

  @Test
  void latestContinuosExecutionDate() {

    when(storageDao.getLatestContinuosExecution()).thenReturn(TestCostant.LOGDATE);

    LocalDate latestConDate = service.getLatestContinuosExecutionDate();

    verify(storageDao, times(1)).getLatestContinuosExecution();
    assertNotNull(latestConDate);
    assertEquals(TestCostant.LOGDATE, latestConDate);
  }

  @Test
  void storageExecutionBetween() {

    when(storageDao.getExecutionBetween(any(), any()))
        .thenReturn(List.of(ExecutionEntity.builder().logFileTypes(LogFileType.valuesAsString())
            .retentionResult(StorageDaoLogicSupport.defaultResultMap())
            .logDate(TestCostant.LOGDATE.toString()).build()));
    List<StorageExecution> resList =
        service.getStorageExecutionBetween(TestCostant.LOGDATE_FROM, TestCostant.LOGDATE);

    verify(storageDao, times(1)).getExecutionBetween(any(), any());

    assertNotNull(resList);
    assertEquals(1, resList.size());
  }

}
