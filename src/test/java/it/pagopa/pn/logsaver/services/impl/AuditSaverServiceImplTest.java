package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.TestUtils;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.DailySaverResultList;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import it.pagopa.pn.logsaver.services.LogFileProcessorService;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.utils.DateUtils;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;

@ExtendWith(MockitoExtension.class)
class AuditSaverServiceImplTest {

  @Mock
  private LogFileReaderService readerService;
  @Mock
  private LogFileProcessorService procService;
  @Mock
  private StorageService storageService;
  @Mock
  private LogSaverCfg cfg;

  private AuditSaverService service;

  @Captor
  private ArgumentCaptor<DailyContextCfg> ctxCaptor;

  @BeforeEach
  void setUp() {
    service = new AuditSaverServiceImpl(readerService, procService, storageService, cfg);
    when(cfg.getTmpBasePath()).thenReturn("/tmp/");
  }

  @Test
  void dailySaverFromLatestExecutionToYesterday() {

    when(storageService.getLatestContinuosExecutionDate())
        .thenReturn(DateUtils.yesterday().minusDays(3));
    when(storageService.getStorageExecutionBetween(any(), any()))
        .thenReturn(List.of(StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(3))
            .details(TestUtils.defaultExecutionDetails()).build()));

    when(readerService.findLogFiles(ctxCaptor.capture())).thenReturn(TestCostant.items.stream());

    when(procService.process(any(), any())).thenReturn(TestCostant.auditFiles);

    when(storageService.store(any(), any())).thenReturn(TestCostant.auditStorage);


    DailySaverResultList res = service.dailySaverFromLatestExecutionToYesterday(
        Set.of(LogFileType.values()), LogSaverUtils.defaultRetentionExportTypeMap());

    List<DailyContextCfg> ctxList = ctxCaptor.getAllValues();

    assertEquals(3, res.getResults().size());

    ctxList.forEach(ctx -> {
      assertEquals(Set.of(Retention.values()), ctx.retentionExportTypeMap().keySet());
      ctx.retentionExportTypeMap().values().forEach(expSet -> {
        assertEquals(Set.of(ExportType.values()), expSet);
      });
    });

  }



  @Test
  void dailySaverFromLatestExecutionToYesterday_WithOneDayToRecover() {

    List<StorageExecution> mockResList = List.of(
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(4))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(2))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(1))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday()).details(TestUtils.defaultExecutionDetails()).build());

    when(storageService.getLatestContinuosExecutionDate())
        .thenReturn(DateUtils.yesterday().minusDays(4));
    when(storageService.getStorageExecutionBetween(any(), any())).thenReturn(mockResList);

    when(readerService.findLogFiles(ctxCaptor.capture())).thenReturn(TestCostant.items.stream());

    when(procService.process(any(), any())).thenReturn(TestCostant.auditFiles);

    when(storageService.store(any(), any())).thenReturn(TestCostant.auditStorage);


    DailySaverResultList res = service.dailySaverFromLatestExecutionToYesterday(
        Set.of(LogFileType.values()), LogSaverUtils.defaultRetentionExportTypeMap());

    List<DailyContextCfg> ctxList = ctxCaptor.getAllValues();

    assertEquals(1, res.getResults().size());

    DailyContextCfg ctx = ctxList.get(0);
    assertEquals(Set.of(Retention.values()), ctx.retentionExportTypeMap().keySet());
    ctx.retentionExportTypeMap().values().forEach(expSet -> {
      assertEquals(Set.of(ExportType.values()), expSet);
    });


  }



  @Test
  void dailySaverFromLatestExecutionToYesterday_WithOneDayToPartialRecover() {

    List<StorageExecution> mockResList = List.of(
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(4))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(2))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(3))
            .details(TestUtils.defaultErrorExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(1))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday()).details(TestUtils.defaultExecutionDetails()).build());

    when(storageService.getLatestContinuosExecutionDate())
        .thenReturn(DateUtils.yesterday().minusDays(4));
    when(storageService.getStorageExecutionBetween(any(), any())).thenReturn(mockResList);

    when(readerService.findLogFiles(ctxCaptor.capture())).thenReturn(TestCostant.items.stream());

    when(procService.process(any(), any())).thenReturn(TestCostant.auditFiles);

    when(storageService.store(any(), any())).thenReturn(TestCostant.auditStorage);


    DailySaverResultList res = service.dailySaverFromLatestExecutionToYesterday(
        Set.of(LogFileType.values()), LogSaverUtils.defaultRetentionExportTypeMap());

    List<DailyContextCfg> ctxList = ctxCaptor.getAllValues();

    assertEquals(1, res.getResults().size());

    DailyContextCfg ctx = ctxList.get(0);
    assertEquals(Set.of(Retention.AUDIT10Y), ctx.retentionExportTypeMap().keySet());
    ctx.retentionExportTypeMap().values().forEach(expSet -> {
      assertEquals(Set.of(ExportType.PDF_SIGNED), expSet);
    });


  }

  @Test
  void dailySaverFromLatestExecutionToYesterday_WithException() {

    List<StorageExecution> mockResList = List.of(

        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(2))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday().minusDays(1))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().logFileTypes(Set.of(LogFileType.values()))
            .logDate(DateUtils.yesterday()).details(TestUtils.defaultErrorExecutionDetails())
            .build());

    when(storageService.getLatestContinuosExecutionDate())
        .thenReturn(DateUtils.yesterday().minusDays(2));
    when(storageService.getStorageExecutionBetween(any(), any())).thenReturn(mockResList);

    when(readerService.findLogFiles(ctxCaptor.capture())).thenReturn(TestCostant.items.stream());

    when(procService.process(any(), any())).thenThrow(FileSystemException.class);


    DailySaverResultList res = service.dailySaverFromLatestExecutionToYesterday(
        Set.of(LogFileType.values()), LogSaverUtils.defaultRetentionExportTypeMap());

    // List<DailyContextCfg> ctxList = ctxCaptor.getAllValues();

    assertEquals(2, res.getResults().size()); // also yesterday

    assertTrue(res.getResults().get(0).hasErrors());


  }
}
