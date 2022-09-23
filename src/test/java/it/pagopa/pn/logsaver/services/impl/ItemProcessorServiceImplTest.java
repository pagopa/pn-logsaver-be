package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.ItemProcessorService;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;

@ExtendWith(MockitoExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class ItemProcessorServiceImplTest {
  @Mock
  private LogSaverCfg cfg;
  @Mock
  private ItemReaderService s3Service;

  private ItemProcessorService service;

  // private MockedStatic<FilesUtils> fileUtils = mockStatic(FilesUtils.class);

  @Captor
  private ArgumentCaptor<String> fileName;

  @Captor
  private ArgumentCaptor<InputStream> content;

  @Captor
  private ArgumentCaptor<Path> tmpPath;

  private FileSystem fileSystem;

  @BeforeEach
  void setUp() {
    this.service = new ItemProcessorServiceImpl(cfg, s3Service);
    this.fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform());
  }

  // @AfterAll
  void destroy() {

  }

  // @Test
  void process() {
    InputStream buckeFile = IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset());
    InputStream file_1_1 =
        IOUtils.toInputStream(RandomStringUtils.random(20), Charset.defaultCharset());
    InputStream file_1_2 =
        IOUtils.toInputStream(RandomStringUtils.random(20), Charset.defaultCharset());
    InputStream file_1_3 =
        IOUtils.toInputStream(RandomStringUtils.random(20), Charset.defaultCharset());
    List<ItemChildren> mockedItemChildrenContent =
        List.of(new ItemChildren(Retention.AUDIT10Y, file_1_1),
            new ItemChildren(Retention.AUDIT5Y, file_1_2),
            new ItemChildren(Retention.DEVELOPER, file_1_3));


    when(s3Service.getItemContent(TestCostant.S3_KEY)).thenReturn(buckeFile);

    when(cfg.filter(any(), any(), any()))
        .thenAnswer(invocation -> mockedItemChildrenContent.stream());
    // .thenReturn(mockedItemChildrenContent.stream());

    Path tmpPath = fileSystem.getPath(TestCostant.TMP_FOLDER);


    List<Item> items = TestCostant.items;

    DailyContextCfg dailyCtx = null;
    // DailyContextCfg.of(TestCostant.LOGDATE, tmpPath.toString(), List.of(ItemType.values()),
    // ExportType.PDF_SIGNED);
    // dailyCtx.initContext();

    List<AuditFile> res = service.process(items, dailyCtx);

    int exepectedFileWrite = items.size() * mockedItemChildrenContent.size();
    verify(s3Service, times(items.size())).getItemContent(anyString());

    Path tmpPathRes = fileSystem.getPath(TestCostant.TMP_FOLDER, TestCostant.LOGDATE.toString());

    File[] foldersRetention = tmpPathRes.toFile().listFiles();

    assertEquals(Retention.values().length, foldersRetention.length);

    for (File folder : foldersRetention) {
      assertTrue(folder.isDirectory());
    }

    // fileUtils.verify(() -> FilesUtils.createOrCleanDirectory(any()), times(1));
    // fileUtils.verify(() -> FilesUtils.createDirectories(any()), times(1));
    // fileUtils.verify(() -> FilesUtils.writeFile(any(), any(), any()), times(3));
    // fileUtils.close();
  }
}
