package it.pagopa.pn.logsaver.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileType;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class LogFileReaderServiceImplTest {

  @Mock
  private S3BucketClient clientS3;
  @Mock
  private LogSaverCfg cfg;

  private LogFileReaderService service;

  @Captor
  private ArgumentCaptor<String> prefix;

  @Captor
  private ArgumentCaptor<String> subFolderPrefix;

  private final List<String> expectedPrefix = TestCostant.EXPECTED_PREFIX;

  @BeforeEach
  void setUp() {
    this.service = new LogFileReaderServiceImpl(clientS3, cfg);
  }

  void mockCfgBase() {
    when(cfg.getLogsRootPathTemplate()).thenReturn("'logs/ecs/%s/'yyyy/MM/dd");
    when(cfg.getCdcRootPathTemplate()).thenReturn("'cdc/%s/'yyyy/MM/dd");
  }

  @Test
  void findItems_WithTableAndMicroserviceByCfg() {
    mockCfgBase();
    List<S3Object> mockResList = List.of(S3Object.builder().key(TestCostant.S3_KEY).build());
    int expFindObjectInvocation = expectedPrefix.size() * mockResList.size();

    when(cfg.getCdcTables()).thenReturn(TestCostant.TABLES);
    when(cfg.getLogsMicroservice()).thenReturn(TestCostant.MICROSERVICES);
    when(clientS3.findObjects(anyString()))
        .thenAnswer((InvocationOnMock invocation) -> mockResList.stream());

    List<LogFileReference> res = service.findLogFiles(TestCostant.CTX).collect(Collectors.toList());

    verify(clientS3, times(0)).findSubFolders(any(String.class), any(String.class));
    verify(clientS3, times(expFindObjectInvocation)).findObjects(prefix.capture());

    List<String> prefixRes = prefix.getAllValues();

    assertEquals(expectedPrefix.size(), prefixRes.size());

    expectedPrefix.stream().forEach(expectedPrefix -> {
      assertThat(prefixRes).contains(expectedPrefix);
    });

    assertEquals(expectedPrefix.size(), res.size());
    LogFileReference defItem = res.get(0);
    assertEquals(TestCostant.S3_KEY, defItem.getS3Key());
    assertEquals(TestCostant.CTX.logDate(), defItem.getLogDate());
    assertNotNull(defItem.getType());

  }

  @Test
  void findItems_WithoutTableAndMicroserviceByCfg() {
    mockCfgBase();
    List<S3Object> mockResList = List.of(S3Object.builder().key(TestCostant.S3_KEY).build());
    int expFindObjectInvocation = expectedPrefix.size() * mockResList.size();

    when(cfg.getCdcTables()).thenReturn(List.of());
    when(cfg.getLogsMicroservice()).thenReturn(List.of());
    when(clientS3.findObjects(anyString()))
        .thenAnswer((InvocationOnMock invocation) -> mockResList.stream());


    when(clientS3.findSubFolders("cdc/", "2022")).thenReturn(TestCostant.TABLES.stream());
    when(clientS3.findSubFolders("logs/", "2022")).thenReturn(TestCostant.MICROSERVICES.stream());

    List<LogFileReference> res = service.findLogFiles(TestCostant.CTX).collect(Collectors.toList());

    verify(clientS3, times(LogFileType.values().length)).findSubFolders(subFolderPrefix.capture(),
        anyString());
    verify(clientS3, times(expFindObjectInvocation)).findObjects(prefix.capture());

    List<String> prefixRes = prefix.getAllValues();
    assertEquals(expectedPrefix.size(), prefixRes.size());
    expectedPrefix.stream().forEach(expectedPrefix -> {
      assertThat(prefixRes).contains(expectedPrefix);
    });


    List<String> subFolderRes = subFolderPrefix.getAllValues();
    assertEquals(LogFileType.values().length, subFolderRes.size());
    Stream.of("cdc/", "logs/").forEach(subFolder -> {
      assertThat(subFolderRes).contains(subFolder);
    });

    assertEquals(expectedPrefix.size(), res.size());
    LogFileReference defItem = res.get(0);
    assertEquals(TestCostant.S3_KEY, defItem.getS3Key());
    assertEquals(TestCostant.CTX.logDate(), defItem.getLogDate());
    assertNotNull(defItem.getType());

  }


  @Test
  void getItemContent() throws IOException {
    String mockContent = "TEST";
    when(clientS3.getObjectContent(anyString()))
        .thenReturn(IOUtils.toInputStream(mockContent, Charset.defaultCharset()));
    InputStream res = service.getContent("test");

    assertEquals(mockContent, IOUtils.toString(res, Charset.defaultCharset()));
  }
}
