package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator;

class LogFileProcessorServiceImplRetentionTest {

  private static final int FILES = 60;
  private static final int BODY_BYTES = 2 * 1024 * 1024;
  private static final long TOTAL_BODY_BYTES = (long) FILES * BODY_BYTES;
  private static final long MAX_TOLERATED_PEAK = 50L * 1024 * 1024;
  private static final int PROBE_EVERY = 10;
  private static final int GC_ATTEMPTS = 4;

  private final java.util.concurrent.atomic.AtomicInteger seen =
      new java.util.concurrent.atomic.AtomicInteger();
  private final java.util.concurrent.atomic.AtomicLong peakDuringProcessing =
      new java.util.concurrent.atomic.AtomicLong();

  private Object originalLogsFilter;

  @BeforeEach
  void setUp() {
    originalLogsFilter = ReflectionTestUtils.getField(LogFileType.LOGS, "filter");
    LogFileType.LogFilter probingFilter =
        (in, content, c) -> {
          if (seen.incrementAndGet() % PROBE_EVERY == 0) {
            peakDuringProcessing.accumulateAndGet(usedHeapAfterGc(), Math::max);
          }
          return Stream.empty();
        };
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", probingFilter);
  }

  @AfterEach
  void tearDown() {
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", originalLogsFilter);
  }

  @Test
  void prefetch_doesNotRetainBodiesOfAlreadyProcessedFiles() {
    LogSaverCfg cfg = new LogSaverCfg();
    ReflectionTestUtils.setField(cfg, "processPrefetch", 8);
    ReflectionTestUtils.setField(cfg, "processPrefetchMaxBytes", DataSize.ofMegabytes(32));

    LogFileProcessorServiceImpl service = new LogFileProcessorServiceImpl(reader(), cfg);
    StreamingExportCoordinator coordinator = Mockito.mock(StreamingExportCoordinator.class);

    long before = usedHeapAfterGc();
    assertDoesNotThrow(() -> service.process(flatMappedInput(), ctx(), coordinator));
    long peakGrowth = peakDuringProcessing.get() - before;

    assertTrue(peakGrowth < MAX_TOLERATED_PEAK,
        "i corpi dei file gia' lavorati restano in memoria durante la lavorazione: picco di heap vivo "
            + (peakGrowth / 1024 / 1024) + " MB mentre si elaborano " + FILES + " file da "
            + (BODY_BYTES / 1024 / 1024) + " MB (" + (TOTAL_BODY_BYTES / 1024 / 1024)
            + " MB complessivi, di cui al massimo 16 MB dovrebbero essere in volo); tollerati "
            + (MAX_TOLERATED_PEAK / 1024 / 1024) + " MB");
  }

  private Stream<LogFileReference> flatMappedInput() {
    return Stream.of(0)
        .flatMap(chunk -> IntStream.range(0, FILES)
            .mapToObj(i -> LogFileReference.builder().logDate(TestCostant.LOGDATE)
                .type(LogFileType.LOGS).s3Key("chunk-" + chunk + "-file-" + i).size(BODY_BYTES)
                .build()));
  }

  private LogFileReaderService reader() {
    return new LogFileReaderService() {
      @Override
      public Stream<LogFileReference> findLogFiles(DailyContextCfg dailyCtx) {
        return Stream.empty();
      }

      @Override
      public InputStream getContent(String key) {
        return new ByteArrayInputStream(new byte[BODY_BYTES]);
      }

      @Override
      public List<AuditStorageEntity> findLogFilesByResult(String result) {
        return List.of();
      }
    };
  }

  private static DailyContextCfg ctx() {
    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.LOGS))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();
    return ctx;
  }

  private static long usedHeapAfterGc() {
    Runtime runtime = Runtime.getRuntime();
    long used = Long.MAX_VALUE;
    for (int i = 0; i < GC_ATTEMPTS; i++) {
      System.gc();
      try {
        Thread.sleep(80);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      used = Math.min(used, runtime.totalMemory() - runtime.freeMemory());
    }
    return used;
  }
}
