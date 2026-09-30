package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator;

class LogFileProcessorServiceImplPrefetchTest {

  private static final long RENDEZVOUS_TIMEOUT_MS = 2_000L;

  private Object originalLogsFilter;

  @BeforeEach
  void setUp() {
    originalLogsFilter = ReflectionTestUtils.getField(LogFileType.LOGS, "filter");
    LogFileType.LogFilter oneFragmentPerFile =
        (in, content, c) -> Stream.of(new ClassifiedLogFragment(Retention.AUDIT10Y,
            "CONTENT".getBytes(Charset.defaultCharset()), in.getFileName()));
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", oneFragmentPerFile);
  }

  @AfterEach
  void tearDown() {
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", originalLogsFilter);
  }

  @Test
  void prefetch_preservesEntryOrder_andMatchesSequentialOutput() {
    List<String> keys = keys(12);

    List<String> sequential = runCollectingNames(keys, 1, key -> body());
    List<String> prefetched = runCollectingNames(keys, 4, key -> body());

    assertEquals(keys, sequential, "il ramo sequenziale deve conservare l'ordine di input");
    assertEquals(sequential, prefetched,
        "il ramo con prefetch deve produrre le stesse entry, nello stesso ordine");
  }

  @Test
  void prefetch_isolatesSingleDownloadError_andContinuesWithOtherFiles() {
    List<String> keys = List.of("good-1", "bad", "good-2");

    List<String> accepted = runCollectingNames(keys, 4, key -> {
      if ("bad".equals(key)) {
        throw new IllegalStateException("corrupt file");
      }
      return body();
    });

    assertEquals(List.of("good-1", "good-2"), accepted,
        "il file in errore non deve fermare gli altri");
  }

  @Test
  void prefetch_keepsFilesInFlightWithinConfiguredBound() {
    int bound = 4;
    AtomicInteger inFlight = new AtomicInteger(0);
    AtomicInteger maxInFlight = new AtomicInteger(0);

    CountDownLatch rendezvous = new CountDownLatch(bound);
    List<String> keys = keys(24);
    runCollectingNames(keys, bound, DataSize.ofMegabytes(32), 1_000L,
        key -> trackedBody(inFlight, maxInFlight, rendezvous));

    assertTrue(maxInFlight.get() <= bound,
        "download simultanei " + maxInFlight.get() + " oltre il limite " + bound);
    assertTrue(maxInFlight.get() > 1,
        "nessuna concorrenza osservata: max in volo " + maxInFlight.get());
  }

  @Test
  void prefetchDisabled_doesNotDownloadAhead() {
    AtomicInteger inFlight = new AtomicInteger(0);
    AtomicInteger maxInFlight = new AtomicInteger(0);

    runCollectingNames(keys(8), 1, key -> {
      int current = inFlight.incrementAndGet();
      maxInFlight.accumulateAndGet(current, Math::max);
      inFlight.decrementAndGet();
      return body();
    });

    assertEquals(1, maxInFlight.get(), "con prefetch=1 nessun file deve essere scaricato in anticipo");
  }

  @Test
  void prefetch_unknownFileSize_isTreatedAsCostly_andProcessedAlone() {
    AtomicInteger inFlight = new AtomicInteger(0);
    AtomicInteger maxInFlight = new AtomicInteger(0);

    CountDownLatch rendezvous = new CountDownLatch(2);
    runCollectingNames(keys(3), 8, DataSize.ofBytes(4_000), 0L,
        key -> trackedBody(inFlight, maxInFlight, rendezvous));

    assertEquals(1, maxInFlight.get(),
        "una dimensione ignota deve costare l'intero budget: in volo contemporaneamente "
            + maxInFlight.get() + " file di dimensione non dichiarata");
  }


  private List<String> runCollectingNames(List<String> keys, int prefetch,
      java.util.function.Function<String, InputStream> bodySupplier) {
    return runCollectingNames(keys, prefetch, DataSize.ofMegabytes(32), 0L, bodySupplier);
  }

  private List<String> runCollectingNames(List<String> keys, int prefetch, DataSize maxBytes,
      long fileSize, java.util.function.Function<String, InputStream> bodySupplier) {
    LogSaverCfg cfg = new LogSaverCfg();
    ReflectionTestUtils.setField(cfg, "processPrefetch", prefetch);
    ReflectionTestUtils.setField(cfg, "processPrefetchMaxBytes", maxBytes);

    List<String> accepted = new ArrayList<>();
    StreamingExportCoordinator coordinator = mock(StreamingExportCoordinator.class);
    Mockito.doAnswer(invocation -> {
      ClassifiedLogFragment fragment = invocation.getArgument(0);
      accepted.add(fragment.getFileName());
      return null;
    }).when(coordinator).accept(any());

    LogFileProcessorServiceImpl service =
        new LogFileProcessorServiceImpl(reader(bodySupplier), cfg);

    List<LogFileReference> refs = keys.stream()
        .map(key -> LogFileReference.builder().logDate(TestCostant.LOGDATE)
            .type(LogFileType.LOGS).s3Key(key).size(fileSize).build())
        .toList();

    assertDoesNotThrow(() -> service.process(refs.stream(), ctx(), coordinator));
    return accepted;
  }

  @Test
  void prefetch_byteBudgetBindsBeforeFileCount() {
    AtomicInteger inFlight = new AtomicInteger(0);
    AtomicInteger maxInFlight = new AtomicInteger(0);

    CountDownLatch rendezvous = new CountDownLatch(4);
    runCollectingNames(keys(16), 8, DataSize.ofBytes(4_000), 1_000L,
        key -> trackedBody(inFlight, maxInFlight, rendezvous));

    assertTrue(maxInFlight.get() <= 4, "il budget in byte non e' stato rispettato: max in volo "
        + maxInFlight.get() + " con budget 4000 byte e file da 1000 byte");
  }

  @Test
  void prefetch_doesNotStall_whenSingleFileExceedsWholeBudget() {
    AtomicInteger inFlight = new AtomicInteger(0);
    AtomicInteger maxInFlight = new AtomicInteger(0);
    List<String> keys = keys(6);

    List<String> accepted = assertTimeoutPreemptively(Duration.ofSeconds(30),
        () -> runCollectingNames(keys, 8, DataSize.ofBytes(1_000), 10_000L,
            key -> trackedBody(inFlight, maxInFlight, new CountDownLatch(1))),
        "un file piu' grande dell'intero budget ha bloccato la lavorazione");

    assertEquals(keys, accepted, "nessun file deve essere perso quando eccede il budget");
    assertEquals(1, maxInFlight.get(),
        "un file che da solo supera il budget deve essere lavorato da solo");
  }

  private static InputStream trackedBody(AtomicInteger inFlight, AtomicInteger maxInFlight,
      CountDownLatch rendezvous) {
    int current = inFlight.incrementAndGet();
    maxInFlight.accumulateAndGet(current, Math::max);
    rendezvous.countDown();
    try {
      rendezvous.await(RENDEZVOUS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    inFlight.decrementAndGet();
    return body();
  }

  private LogFileReaderService reader(java.util.function.Function<String, InputStream> bodySupplier) {
    return new LogFileReaderService() {
      @Override
      public Stream<LogFileReference> findLogFiles(DailyContextCfg dailyCtx) {
        return Stream.empty();
      }

      @Override
      public InputStream getContent(String key) {
        return bodySupplier.apply(key);
      }

      @Override
      public List<AuditStorageEntity> findLogFilesByResult(String result) {
        return List.of();
      }
    };
  }

  private static InputStream body() {
    return IOUtils.toInputStream("BUCKETFILE", Charset.defaultCharset());
  }

  private static List<String> keys(int n) {
    return IntStream.range(0, n).mapToObj(i -> String.format("file-%02d", i)).toList();
  }

  private static DailyContextCfg ctx() {
    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.LOGS))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();
    return ctx;
  }
}
