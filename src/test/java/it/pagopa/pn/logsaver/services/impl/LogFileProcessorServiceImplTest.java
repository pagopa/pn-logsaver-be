package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.LogFileProcessorService;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.services.functions.ExportAudit;
import it.pagopa.pn.logsaver.services.impl.functions.ExportAuditPdf;
import it.pagopa.pn.logsaver.services.impl.functions.ExportAuditZip;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;

@ExtendWith(MockitoExtension.class)
class LogFileProcessorServiceImplTest {

  @Mock
  private LogFileReaderService s3Service;

  private LogFileProcessorService service;

  @Mock
  private LogSaverCfg cfg;

  @Mock
  private InputStream content;


  @BeforeEach
  void setUp() {
    Map<String, ExportAudit> exportFactory = Map.of(ExportType.PDF_S, new ExportAuditPdf(cfg),
        ExportType.ZIP_S, new ExportAuditZip(cfg));
    this.service = new LogFileProcessorServiceImpl(s3Service, exportFactory);
  }

  @AfterEach
  void destroy() throws IOException {}

  @Test
  void process() throws InterruptedException, ExecutionException {
    when(cfg.getMaxSize()).thenReturn(DataSize.of(1, DataUnit.MEGABYTES));
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> noOpFilter =
        (in, cfg) -> childrenList().stream();
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", noOpFilter);
    ReflectionTestUtils.setField(LogFileType.CDC, "filter", noOpFilter);

    when(s3Service.getContent(TestCostant.S3_KEY))
        .then((i) -> IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset()));


    List<LogFileReference> items = TestCostant.items;
    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.values()))
        .logDate(TestCostant.LOGDATE).build();


    try (MockedStatic<FilesUtils> fileUtils = mockStatic(FilesUtils.class);
        MockedStatic<LogSaverUtils> logSaverUtils = mockStatic(LogSaverUtils.class);) {

      logSaverUtils.when(() -> LogSaverUtils.toParallelStream(any()))
          .thenAnswer(in -> in.getArgument(0, List.class).stream());

      ctx.initContext();

      List<AuditFile> res = service.process(items.stream(), ctx);

      int exepectedFileAudite = ctx.retentionExportTypeMap().values().stream().reduce(0,
          (prev, e) -> prev + e.size(), Integer::sum);
      assertEquals(exepectedFileAudite, res.size());
      ctx.retentionExportTypeMap().keySet().forEach(retention -> {
        Set<ExportType> expectedExportType = ctx.getExportTypesByRetention(retention);
        Set<ExportType> resultExportType =
            res.stream().filter(file -> file.retention() == retention).map(AuditFile::exportType)
                .collect(Collectors.toSet());

        assertEquals(expectedExportType, resultExportType);

      });

      int exepectedFileWrite = items.size() * childrenList().size();
      verify(s3Service, times(items.size())).getContent(anyString());

      fileUtils.verify(() -> FilesUtils.createOrCleanDirectory(any()), times(1));
      fileUtils.verify(() -> FilesUtils.createDirectories(any()), times(1));
      fileUtils.verify(() -> FilesUtils.writeFile(any(), any(), any()), times(exepectedFileWrite));

    }
  }


  @Test
  void process_IOExceptionWhenCloseS3Stream() throws IOException {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> noOpFilter =
        (in, cfg) -> childrenList().stream();
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", noOpFilter);
    ReflectionTestUtils.setField(LogFileType.CDC, "filter", noOpFilter);
    doThrow(IOException.class).when(content).close();
    when(s3Service.getContent(TestCostant.S3_KEY)).then((i) -> content);
    Stream<LogFileReference> items = TestCostant.items.stream();
    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.values()))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();
    assertThrows(UncheckedIOException.class, () -> service.process(items, ctx));
  }

  @Test
  void process_IOExceptionWhenCloseFileStream() throws IOException {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> noOpFilter =
        (in, cfg) -> childrenListIOException().stream();
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", noOpFilter);
    ReflectionTestUtils.setField(LogFileType.CDC, "filter", noOpFilter);
    when(content.read(any())).thenReturn(-1);
    doThrow(IOException.class).when(content).close();
    when(s3Service.getContent(TestCostant.S3_KEY))
        .then((i) -> IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset()));
    Stream<LogFileReference> items = TestCostant.items.stream();

    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.values()))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();
    assertThrows(UncheckedIOException.class, () -> service.process(items, ctx));
  }


  private List<ClassifiedLogFragment> childrenListIOException() {
    return List.of(new ClassifiedLogFragment(Retention.AUDIT10Y, content, "fileName"));
  }

  private List<ClassifiedLogFragment> childrenList() {
    InputStream file_1_1 =
        IOUtils.toInputStream(RandomStringUtils.random(20), Charset.defaultCharset());
    InputStream file_1_2 =
        IOUtils.toInputStream(RandomStringUtils.random(20), Charset.defaultCharset());
    InputStream file_1_3 =
        IOUtils.toInputStream(RandomStringUtils.random(20), Charset.defaultCharset());
    return List.of(new ClassifiedLogFragment(Retention.AUDIT10Y, file_1_1, "fileName10"),
        new ClassifiedLogFragment(Retention.AUDIT5Y, file_1_2, "fileName5"),
        new ClassifiedLogFragment(Retention.DEVELOPER, file_1_3, "fileNamedev"));

  }
}
