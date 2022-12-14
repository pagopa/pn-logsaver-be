package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.exceptions.ExternalException;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.DailyAuditDownloadable;
import it.pagopa.pn.logsaver.model.DailyDownloadResultList;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import it.pagopa.pn.logsaver.services.StorageService;

@ExtendWith(MockitoExtension.class)
class AuditDownloadServiceImplTest {

  @Mock
  private S3BucketClient clientS3;

  @Mock
  private StorageService storageService;
  private AuditDownloadServiceImpl service;

  @Captor
  private ArgumentCaptor<String> key;

  @Captor
  private ArgumentCaptor<String> subFolderPrefix;


  @BeforeEach
  void setUp() {
    this.service = new AuditDownloadServiceImpl(clientS3, storageService);
  }

  @Test
  void downloadAudits() {
    AuditDownloadReference file = AuditDownloadReference.builder().logDate(TestCostant.LOGDATE)
        .status(AuditStorageStatus.SENT).downloadUrl("http://test").uploadKey("updKey").build();
    DailyAuditDownloadable dailyAuditStorage =
        DailyAuditDownloadable.builder().logDate(TestCostant.LOGDATE).audits(List.of(file)).build();
    when(storageService.getAuditFile(any(), any())).thenReturn(List.of(dailyAuditStorage));
    when(storageService.dowloadAuditFile(any(), any())).thenReturn(file);

    doNothing().when(clientS3).uploadContent(key.capture(), any(), anyLong(), any());

    DailyDownloadResultList result =
        service.downloadAudits(TestCostant.LOGDATE, TestCostant.LOGDATE, null);

    verify(storageService, times(1)).getAuditFile(any(), any());
    verify(storageService, times(1)).dowloadAuditFile(any(), any());
    verify(clientS3, times(1)).uploadContent(any(), any(), anyLong(), any());

    assertTrue(
        key.getAllValues().stream().filter(str -> str.contains(".csv")).findFirst().isPresent());


    assertNotNull(result);
    assertNotNull(result.getResults());
    assertEquals(1, result.getResults().size());
    assertNull(result.getResults().get(0).getError());
  }

  @Test
  void downloadAudits_WithDestFolder() {
    AuditDownloadReference file = AuditDownloadReference.builder().logDate(TestCostant.LOGDATE)
        .status(AuditStorageStatus.SENT).downloadUrl("http://test").uploadKey("updKey").build();
    DailyAuditDownloadable dailyAuditStorage =
        DailyAuditDownloadable.builder().logDate(TestCostant.LOGDATE).audits(List.of(file)).build();
    when(storageService.getAuditFile(any(), any())).thenReturn(List.of(dailyAuditStorage));
    when(storageService.dowloadAuditFile(any(), any())).thenReturn(file);

    doNothing().when(clientS3).uploadContent(key.capture(), any(), anyLong(), any());

    DailyDownloadResultList result =
        service.downloadAudits(TestCostant.LOGDATE, TestCostant.LOGDATE, "destination/");

    verify(storageService, times(1)).getAuditFile(any(), any());
    verify(storageService, times(1)).dowloadAuditFile(any(), any());
    verify(clientS3, times(1)).uploadContent(any(), any(), anyLong(), any());

    assertTrue(key.getAllValues().stream().filter(str -> str.contains("destination/")).findFirst()
        .isPresent());
    assertNotNull(result);
    assertNotNull(result.getResults());
    assertEquals(1, result.getResults().size());
    assertNull(result.getResults().get(0).getError());

  }

  @Test
  void downloadAudits_Error() {
    AuditDownloadReference file = AuditDownloadReference.builder().logDate(TestCostant.LOGDATE)
        .status(AuditStorageStatus.SENT).downloadUrl("http://test").uploadKey("updKey").build();
    DailyAuditDownloadable dailyAuditStorage =
        DailyAuditDownloadable.builder().logDate(TestCostant.LOGDATE).audits(List.of(file)).build();
    when(storageService.getAuditFile(any(), any())).thenReturn(List.of(dailyAuditStorage));
    when(storageService.dowloadAuditFile(any(), any())).thenThrow(ExternalException.class);



    DailyDownloadResultList result =
        service.downloadAudits(TestCostant.LOGDATE, TestCostant.LOGDATE, null);

    verify(storageService, times(1)).getAuditFile(any(), any());
    verify(storageService, times(1)).dowloadAuditFile(any(), any());
    verify(clientS3, times(0)).uploadContent(any(), any(), anyLong(), any());

    assertNotNull(result);
    assertNotNull(result.getResults());
    assertEquals(1, result.getResults().size());
    assertNotNull(result.getResults().get(0).getError());
  }

  @Test
  void dowloadAuditFileConsumer() {
    AuditDownloadReference file = AuditDownloadReference.builder().size(BigDecimal.valueOf(8L))
        .fileName("fileName").logDate(TestCostant.LOGDATE).status(AuditStorageStatus.SENT)
        .destinationFolder("destination/").downloadUrl("http://test").uploadKey("updKey").build();

    doNothing().when(clientS3).uploadContent(key.capture(), any(), anyLong(), any());

    AuditDownloadReference result = service.dowloadAuditFileConsumer(file);

    verify(clientS3, times(1)).uploadContent(any(), any(), anyLong(), any());

    assertTrue(key.getAllValues().stream().filter(str -> str.equals("destination/fileName"))
        .findFirst().isPresent());
    assertNotNull(result);

  }
}
