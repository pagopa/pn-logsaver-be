package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.LogFileProcessorService;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator.UploadedPart;

@ExtendWith(MockitoExtension.class)
class LogFileProcessorServiceImplTest {

  @Mock
  private LogFileReaderService s3Service;

  @Mock
  private LogSaverCfg cfg;

  @Mock
  private InputStream content;

  @Mock
  private StreamingExportCoordinator coordinator;

  private LogFileProcessorService service;

  @BeforeEach
  void setUp() {
    this.service = new LogFileProcessorServiceImpl(s3Service);
  }

  @Test
  void process() {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> noOpFilter =
        (in, c) -> childrenList().stream();
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", noOpFilter);
    ReflectionTestUtils.setField(LogFileType.CDC, "filter", noOpFilter);

    when(s3Service.getContent(TestCostant.S3_KEY))
        .then(i -> IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset()));
    List<UploadedPart> finishResult = List.of();
    when(coordinator.finish()).thenReturn(finishResult);

    List<LogFileReference> items = TestCostant.items;
    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.values()))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();

    List<UploadedPart> res = service.process(items.stream(), ctx, coordinator);

    assertSame(finishResult, res);
    verify(s3Service, times(items.size())).getContent(anyString());
    verify(coordinator, times(items.size() * childrenList().size())).accept(any());
  }

  @Test
  void process_shouldConsumeInputLazily_notMaterializeBeforeFirstDownload() {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> emptyFilter =
        (in, c) -> Stream.empty();
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", emptyFilter);

    AtomicInteger pulled = new AtomicInteger(0);
    AtomicInteger pulledAtFirstDownload = new AtomicInteger(-1);
    when(s3Service.getContent(anyString())).thenAnswer(i -> {
      pulledAtFirstDownload.compareAndSet(-1, pulled.get());
      return IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset());
    });

    int n = 20;
    List<LogFileReference> refs = IntStream.range(0, n)
        .mapToObj(k -> LogFileReference.builder().logDate(TestCostant.LOGDATE)
            .type(LogFileType.LOGS).s3Key(TestCostant.S3_KEY).build())
        .collect(Collectors.toList());

    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.LOGS))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();

    Stream<LogFileReference> input = refs.stream().peek(r -> pulled.incrementAndGet());
    service.process(input, ctx, coordinator);

    assertTrue(pulledAtFirstDownload.get() >= 0, "getContent mai invocato");
    assertTrue(pulledAtFirstDownload.get() < n,
        "input materializzato prima del primo download (pulled=" + pulledAtFirstDownload.get()
            + "/" + n + ")");
  }

  @Test
  void process_IOExceptionWhenCloseS3Stream_isIsolated() throws IOException {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> noOpFilter =
        (in, c) -> childrenList().stream();
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", noOpFilter);
    ReflectionTestUtils.setField(LogFileType.CDC, "filter", noOpFilter);
    doThrow(IOException.class).when(content).close();
    when(s3Service.getContent(TestCostant.S3_KEY)).then(i -> content);
    Stream<LogFileReference> items = TestCostant.items.stream();
    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.values()))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();
    assertDoesNotThrow(() -> service.process(items, ctx, coordinator));
  }

  @Test
  void process_shouldIsolatePerFileError_andContinueWithOtherFiles() {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> noOpFilter =
        (in, c) -> childrenList().stream();
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", noOpFilter);

    when(s3Service.getContent(anyString())).thenAnswer(i -> {
      String key = i.getArgument(0);
      if ("bad".equals(key)) {
        throw new RuntimeException("corrupt file");
      }
      return IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset());
    });

    List<LogFileReference> items = List.of(
        LogFileReference.builder().logDate(TestCostant.LOGDATE).type(LogFileType.LOGS).s3Key("good-1").build(),
        LogFileReference.builder().logDate(TestCostant.LOGDATE).type(LogFileType.LOGS).s3Key("bad").build(),
        LogFileReference.builder().logDate(TestCostant.LOGDATE).type(LogFileType.LOGS).s3Key("good-2").build());

    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.values()))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();

    assertDoesNotThrow(() -> service.process(items.stream(), ctx, coordinator));
    verify(coordinator, times(2 * childrenList().size())).accept(any());
  }

  @Test
  void process_prefetchGreaterThanOne_producesSameFragmentsInSameOrderAsSequential() {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> keyedFilter =
        (in, c) -> Stream.of(
            new ClassifiedLogFragment(Retention.AUDIT10Y,
                IOUtils.toInputStream("C-" + in.getS3Key(), Charset.defaultCharset()),
                in.getS3Key()));
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", keyedFilter);

    when(s3Service.getContent(anyString()))
        .thenAnswer(i -> IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset()));

    List<String> sequential = collectAcceptedNames(0);
    List<String> parallel = collectAcceptedNames(4);

    assertEquals(20, sequential.size());
    assertEquals(sequential, parallel,
        "il prefetch deve produrre gli stessi frammenti nello stesso ordine del sequenziale");
  }

  @Test
  void process_prefetchGreaterThanOne_cdcFragmentContentIsStillReadableAfterPrepare()
      throws IOException {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> realCdcFilter =
        (in, c) -> Stream.of(
            new ClassifiedLogFragment(Retention.AUDIT10Y, in.getContent(), in.getFileName()));
    ReflectionTestUtils.setField(LogFileType.CDC, "filter", realCdcFilter);
    ReflectionTestUtils.setField(service, "prefetch", 4);
    ReflectionTestUtils.setField(service, "prefetchMaxBytes", DataSize.ofMegabytes(64));

    when(s3Service.getContent(anyString()))
        .thenAnswer(i -> IOUtils.toInputStream("CDC-PAYLOAD", Charset.defaultCharset()));

    List<ClassifiedLogFragment> accepted = new ArrayList<>();
    doAnswer(i -> {
      accepted.add(i.getArgument(0));
      return null;
    }).when(coordinator).accept(any());

    service.process(Stream.of(LogFileReference.builder().logDate(TestCostant.LOGDATE)
        .type(LogFileType.CDC).s3Key("cdc-1").build()), context(Set.of(LogFileType.CDC)),
        coordinator);

    assertEquals(1, accepted.size());
    assertEquals("CDC-PAYLOAD",
        IOUtils.toString(accepted.get(0).getContent(), Charset.defaultCharset()),
        "il contenuto del frammento CDC non deve essere uno stream chiuso");
  }

  @Test
  void process_prefetchMaxBytes_throttlesInFlightBeforeAllDownloadsAreSubmitted() {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> emptyOneFilter =
        (in, c) -> Stream.of(new ClassifiedLogFragment(Retention.AUDIT10Y,
            IOUtils.toInputStream("X", Charset.defaultCharset()), in.getS3Key()));
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", emptyOneFilter);

    assertEquals(20, pulledAtFirstAccept(DataSize.ofMegabytes(64)),
        "senza tetto stretto tutti gli elementi vengono sottomessi prima del primo consumo");
    assertEquals(1, pulledAtFirstAccept(DataSize.ofBytes(10)),
        "con il tetto a byte il consumo deve partire dopo il primo elemento");
  }

  @Test
  void process_prefetchGreaterThanOne_isolatesPerFileError_andContinues() {
    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> failingFilter =
        (in, c) -> {
          if ("bad".equals(in.getS3Key())) {
            throw new IllegalStateException("corrupt file");
          }
          return Stream.of(new ClassifiedLogFragment(Retention.AUDIT10Y,
              IOUtils.toInputStream("X", Charset.defaultCharset()), in.getS3Key()));
        };
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", failingFilter);
    ReflectionTestUtils.setField(service, "prefetch", 4);
    ReflectionTestUtils.setField(service, "prefetchMaxBytes", DataSize.ofMegabytes(64));

    when(s3Service.getContent(anyString()))
        .thenAnswer(i -> IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset()));

    List<LogFileReference> items = List.of(ref("good-1"), ref("bad"), ref("good-2"));

    assertDoesNotThrow(() -> service.process(items.stream(), context(Set.of(LogFileType.LOGS)),
        coordinator));
    verify(coordinator, times(2)).accept(any());
  }

  private List<String> collectAcceptedNames(int prefetch) {
    Mockito.reset(coordinator);
    ReflectionTestUtils.setField(service, "prefetch", prefetch);
    ReflectionTestUtils.setField(service, "prefetchMaxBytes", DataSize.ofMegabytes(64));
    List<String> names = new ArrayList<>();
    doAnswer(i -> {
      names.add(((ClassifiedLogFragment) i.getArgument(0)).getFileName());
      return null;
    }).when(coordinator).accept(any());

    List<LogFileReference> items =
        IntStream.range(0, 20).mapToObj(k -> ref("key-" + k)).collect(Collectors.toList());
    service.process(items.stream(), context(Set.of(LogFileType.LOGS)), coordinator);
    return names;
  }

  private int pulledAtFirstAccept(DataSize maxBytes) {
    Mockito.reset(coordinator, s3Service);
    ReflectionTestUtils.setField(service, "prefetch", 100);
    ReflectionTestUtils.setField(service, "prefetchMaxBytes", maxBytes);

    AtomicInteger pulled = new AtomicInteger(0);
    AtomicInteger atFirstAccept = new AtomicInteger(-1);
    when(s3Service.getContent(anyString()))
        .thenAnswer(i -> IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset()));
    doAnswer(i -> {
      atFirstAccept.compareAndSet(-1, pulled.get());
      return null;
    }).when(coordinator).accept(any());

    List<LogFileReference> items = IntStream.range(0, 20)
        .mapToObj(k -> LogFileReference.builder().logDate(TestCostant.LOGDATE)
            .type(LogFileType.LOGS).s3Key("key-" + k).size(100L).build())
        .collect(Collectors.toList());
    service.process(items.stream().peek(r -> pulled.incrementAndGet()),
        context(Set.of(LogFileType.LOGS)), coordinator);
    return atFirstAccept.get();
  }

  private LogFileReference ref(String key) {
    return LogFileReference.builder().logDate(TestCostant.LOGDATE).type(LogFileType.LOGS)
        .s3Key(key).size(1L).build();
  }

  private DailyContextCfg context(Set<LogFileType> types) {
    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(types).logDate(TestCostant.LOGDATE)
        .build();
    ctx.initContext();
    return ctx;
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
