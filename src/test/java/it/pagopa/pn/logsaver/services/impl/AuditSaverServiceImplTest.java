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
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.DailySaverResult;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.model.StorageExecution;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import it.pagopa.pn.logsaver.services.ItemProcessorService;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import it.pagopa.pn.logsaver.utils.DateUtils;
import it.pagopa.pn.logsaver.utils.LsUtils;

@ExtendWith(MockitoExtension.class)
class AuditSaverServiceImplTest {

  @Mock
  private ItemReaderService readerService;
  @Mock
  private ItemProcessorService procService;
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

    when(storageService.latestContinuosExecutionDate())
        .thenReturn(DateUtils.yesterday().minusDays(3));
    when(storageService.storageExecutionBetween(any(), any())).thenReturn(List.of(StorageExecution
        .builder().itemTypes(Set.of(ItemType.values())).logDate(DateUtils.yesterday().minusDays(3))
        .details(TestUtils.defaultExecutionDetails()).build()));

    when(readerService.findItems(ctxCaptor.capture())).thenReturn(TestCostant.items);

    when(procService.process(any(), any())).thenReturn(TestCostant.auditFiles);

    when(storageService.store(any(), any())).thenReturn(TestCostant.auditStorage);


    List<DailySaverResult> res = service.dailySaverFromLatestExecutionToYesterday(
        Set.of(ItemType.values()), LsUtils.defaultRetentionExportTypeMap());

    List<DailyContextCfg> ctxList = ctxCaptor.getAllValues();

    assertEquals(3, res.size());

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
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(4))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(2))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(1))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday()).details(TestUtils.defaultExecutionDetails()).build());

    when(storageService.latestContinuosExecutionDate())
        .thenReturn(DateUtils.yesterday().minusDays(4));
    when(storageService.storageExecutionBetween(any(), any())).thenReturn(mockResList);

    when(readerService.findItems(ctxCaptor.capture())).thenReturn(TestCostant.items);

    when(procService.process(any(), any())).thenReturn(TestCostant.auditFiles);

    when(storageService.store(any(), any())).thenReturn(TestCostant.auditStorage);


    List<DailySaverResult> res = service.dailySaverFromLatestExecutionToYesterday(
        Set.of(ItemType.values()), LsUtils.defaultRetentionExportTypeMap());

    List<DailyContextCfg> ctxList = ctxCaptor.getAllValues();

    assertEquals(1, res.size());

    DailyContextCfg ctx = ctxList.get(0);
    assertEquals(Set.of(Retention.values()), ctx.retentionExportTypeMap().keySet());
    ctx.retentionExportTypeMap().values().forEach(expSet -> {
      assertEquals(Set.of(ExportType.values()), expSet);
    });


  }



  @Test
  void dailySaverFromLatestExecutionToYesterday_WithOneDayToPartialRecover() {

    List<StorageExecution> mockResList = List.of(
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(4))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(2))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(3))
            .details(TestUtils.defaultErrorExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(1))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday()).details(TestUtils.defaultExecutionDetails()).build());

    when(storageService.latestContinuosExecutionDate())
        .thenReturn(DateUtils.yesterday().minusDays(4));
    when(storageService.storageExecutionBetween(any(), any())).thenReturn(mockResList);

    when(readerService.findItems(ctxCaptor.capture())).thenReturn(TestCostant.items);

    when(procService.process(any(), any())).thenReturn(TestCostant.auditFiles);

    when(storageService.store(any(), any())).thenReturn(TestCostant.auditStorage);


    List<DailySaverResult> res = service.dailySaverFromLatestExecutionToYesterday(
        Set.of(ItemType.values()), LsUtils.defaultRetentionExportTypeMap());

    List<DailyContextCfg> ctxList = ctxCaptor.getAllValues();

    assertEquals(1, res.size());

    DailyContextCfg ctx = ctxList.get(0);
    assertEquals(Set.of(Retention.AUDIT10Y), ctx.retentionExportTypeMap().keySet());
    ctx.retentionExportTypeMap().values().forEach(expSet -> {
      assertEquals(Set.of(ExportType.PDF_SIGNED), expSet);
    });


  }

  @Test
  void dailySaverFromLatestExecutionToYesterday_WithException() {

    List<StorageExecution> mockResList = List.of(

        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(2))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday().minusDays(1))
            .details(TestUtils.defaultExecutionDetails()).build(),
        StorageExecution.builder().itemTypes(Set.of(ItemType.values()))
            .logDate(DateUtils.yesterday()).details(TestUtils.defaultErrorExecutionDetails())
            .build());

    when(storageService.latestContinuosExecutionDate())
        .thenReturn(DateUtils.yesterday().minusDays(2));
    when(storageService.storageExecutionBetween(any(), any())).thenReturn(mockResList);

    when(readerService.findItems(ctxCaptor.capture())).thenReturn(TestCostant.items);

    when(procService.process(any(), any())).thenThrow(FileSystemException.class);


    List<DailySaverResult> res = service.dailySaverFromLatestExecutionToYesterday(
        Set.of(ItemType.values()), LsUtils.defaultRetentionExportTypeMap());

    // List<DailyContextCfg> ctxList = ctxCaptor.getAllValues();

    assertEquals(1, res.size());

    assertTrue(res.get(0).hasErrors());


  }
}
