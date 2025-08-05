package it.pagopa.pn.logsaver.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.utils.DateUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assert;
import org.junit.jupiter.api.Assertions;
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
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class LogFileReaderServiceImplTest {

  @Mock
  private S3BucketClient clientS3;
  @Mock
  private LogSaverCfg cfg;
  @Mock
  private StorageDao storageDao;
  @Mock
  private StorageService storageService;

  private LogFileReaderService service;

  @Captor
  private ArgumentCaptor<String> prefix;

  @Captor
  private ArgumentCaptor<String> subFolderPrefix;

  private final List<String> expectedPrefix = TestCostant.EXPECTED_PREFIX;

  @BeforeEach
  void setUp() {
    this.service = new LogFileReaderServiceImpl(clientS3, cfg, storageDao, storageService);
  }

  void mockCfgBase() {
    when(cfg.getLogsRootPathTemplate()).thenReturn("'logs/ecs/%s/'yyyy/MM/dd");
    when(cfg.getCdcRootPathTemplate()).thenReturn("'cdc/%s/'yyyy/MM/dd");
    when(cfg.getCdcTables()).thenReturn(List.of("NONE"));
  }

  void mockCfgBaseWithPrefix() {
    when(cfg.getLogsRootPathTemplate()).thenReturn("'logs/ecs/%s/'yyyy/MM/dd");
    when(cfg.getCdcRootPathTemplate()).thenReturn("'cdc/%s/'yyyy/MM/dd");
    when(cfg.getCdcTablesPrefix()).thenReturn("");
  }

  @Test
  void getCdcTablesConfiguration(){
    System.out.println("PARAMETER CdcTables: " + cfg.getCdcTables());
    when(cfg.getCdcTables()).thenReturn(List.of(TestCostant.S3_SUBFOLDER_TO_SCAN_ALL) );
    System.out.println("PARAMETER CdcTables: " + cfg.getCdcTables());
    assertNotNull(cfg.getCdcTables());

    when(cfg.getCdcTablesPrefix()).thenReturn("TABLE_NAME_");

    Stream<String> stream = this.findSubfolders(LogFileType.CDC, LocalDate.of(2023, 8, 4));
    System.out.println("stream : " + stream.toList());
    Assertions.assertEquals(cfg.getCdcTablesPrefix(), "TABLE_NAME_");
  }

  private Stream<String> findSubfolders(LogFileType type, LocalDate logDate) {
    System.out.println("Start search subfolders for log file " + type.name());

    List<String> subFolderListCfg =
            LogFileType.CDC == type ? this.getCdcTables() : cfg.getLogsMicroservice();

    if ( LogFileType.LOGS == type ){
      if (subFolderListCfg.isEmpty()) {// Ricerca delle subFolders su S3
        return findSubfoldersS3(type, logDate);
      }
      return subFolderListCfg.stream();
    } else {
      if (subFolderListCfg.get(0).equals("NONE")) {
        System.out.println("CDC tables non configurate: nessuna scansione verrà eseguita.");
        return Stream.empty(); // Non fa nessuna scansione, torna uno stream vuoto
      } else if (subFolderListCfg.get(0).equals("ALL")) { // Ricerca delle subFolders su S3
        System.out.println("Ricerca di tutte le subFolders su S3");
        return findSubfoldersS3(type, logDate);
      } else {
        return subFolderListCfg.stream();
      }
    }
  }

  private List<String> getCdcTables() {
    return (cfg.getCdcTables() == null || cfg.getCdcTables().isEmpty()) ? List.of("NONE") : cfg.getCdcTables();
  }

  private Stream<String> findSubfoldersS3(LogFileType type, LocalDate logDate) {
    // getCdcRootPathTemplate : 'cdcTos3/%s/'yyyy/MM/dd  --> pathPrefix : cdcTos3/
    //                        : 'logsTos3/'yyyy/MM/dd    --> pathPrefix : logsTos3/
    String pathPrefix = StringUtils.substringBefore(
                    LogFileType.CDC == type ? "'cdcTos3/%s/'yyyy/MM/dd" : "'logsTos3/'yyyy/MM/dd", "/")
            .replace("'", "").concat("/");
System.out.println("pathPrefix: " + pathPrefix);
    // getCdcTablesPrefix : TABLE_NAME_
     String subFolderPrefix = LogFileType.CDC == type ? cfg.getCdcTablesPrefix() : "";
    if(LogFileType.CDC == type ) {
      //pathPrefix = pathPrefix.substring(0, pathPrefix.indexOf("/")+1);
      subFolderPrefix = "";
    }

    System.out.println("subFolderPrefix: " + subFolderPrefix);
    System.out.println("DateUtils.getYear(logDate)): " + DateUtils.getYear(logDate));
    System.out.println("pathPrefix.concat(subFolderPrefix) : {} " + pathPrefix.concat(subFolderPrefix));


    List<String> subFolderList = clientS3.findSubFoldersWithPrefix(pathPrefix, subFolderPrefix).collect(Collectors.toList());
    System.out.println("subFolderList: " + subFolderList);
    if (subFolderList.isEmpty()) {
      return Stream.of("");
    }
    return subFolderList.stream();

    //return Stream.of("");
  }


/*
  @Test
  void findItems_WithTableAndMicroserviceByCfg() {
    mockCfgBase();
    List<S3Object> mockResList = List.of(S3Object.builder().key(TestCostant.S3_KEY).build());
    int expFindObjectInvocation = expectedPrefix.size() * mockResList.size();

      when(cfg.getCdcTables()).thenReturn(List.of(TestCostant.S3_SUBFOLDER_TO_SCAN_NONE) ); //TABLES);
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
    mockCfgBaseWithPrefix();
    List<S3Object> mockResList = List.of(S3Object.builder().key(TestCostant.S3_KEY).build());
    int expFindObjectInvocation = expectedPrefix.size() * mockResList.size();

    when(cfg.getCdcTables()).thenReturn(List.of("NONE"));
    when(cfg.getLogsMicroservice()).thenReturn(List.of());
    when(clientS3.findObjects(anyString()))
        .thenAnswer((InvocationOnMock invocation) -> mockResList.stream());


    when(clientS3.findSubFoldersWithPrefix("cdc/", "","2022")).thenReturn(TestCostant.TABLES.stream());
    when(clientS3.findSubFoldersWithPrefix("logs/", "", "2022")).thenReturn(TestCostant.MICROSERVICES.stream());

    List<LogFileReference> res = service.findLogFiles(TestCostant.CTX).collect(Collectors.toList());

    verify(clientS3, times(LogFileType.values().length)).findSubFoldersWithPrefix(subFolderPrefix.capture(),
            anyString(),
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
*/

  @Test
  void getItemContent() throws IOException {
    String mockContent = "TEST";
    when(clientS3.getObjectContent(anyString()))
        .thenReturn(IOUtils.toInputStream(mockContent, Charset.defaultCharset()));
    InputStream res = service.getContent("test");

    assertEquals(mockContent, IOUtils.toString(res, Charset.defaultCharset()));
  }
}
