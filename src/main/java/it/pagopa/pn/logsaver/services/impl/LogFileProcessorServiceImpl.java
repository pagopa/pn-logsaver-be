package it.pagopa.pn.logsaver.services.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.services.LogFileProcessorService;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator.UploadedPart;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogFileProcessorServiceImpl implements LogFileProcessorService {


  @NonNull
  private final LogFileReaderService s3Service;

  @Value("${log-saver.process.prefetch:1}")
  private int prefetch;

  @Value("${log-saver.process.prefetch-max-bytes:64MB}")
  private DataSize prefetchMaxBytes;

  @Override
  public List<UploadedPart> process(Stream<LogFileReference> fileStream, DailyContextCfg dailyCtx,
      StreamingExportCoordinator coordinator) {
    log.info("Start processing file - start date={}", dailyCtx.logDate());

    // contatori per i thread
    AtomicInteger processedCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

    if (prefetch > 1) {
      processPrefetch(fileStream, dailyCtx, coordinator, processedCount, errorCount);
    } else {
      fileStream.forEach(item ->
              {
                try {
                  downloadFilterAccept(item, dailyCtx, coordinator);
                  processedCount.incrementAndGet();
                } catch (Exception e) {
                  errorCount.incrementAndGet();
                  log.error("ERRORE: Salto il file {} a causa di: {}", item.getS3Key(), e.getMessage());
                }
              }
      );
    }
    log.info("Processing completato. Successi: {}, Errori: {}", processedCount.get(), errorCount.get());

    return coordinator.finish();
  }

  private record PendingWork(LogFileReference ref, long size,
      Future<List<ClassifiedLogFragment>> work) {
  }

  private void processPrefetch(Stream<LogFileReference> fileStream, DailyContextCfg dailyCtx,
      StreamingExportCoordinator coordinator, AtomicInteger processedCount,
      AtomicInteger errorCount) {
    log.info("Processing with prefetch={} maxBytes={} availableProcessors={} on virtual threads",
        prefetch, prefetchMaxBytes.toBytes(), Runtime.getRuntime().availableProcessors());

    ExecutorService pool = newPool();
    Deque<PendingWork> inFlight = new ArrayDeque<>(prefetch);
    long inFlightBytes = 0;
    try {
      Iterator<LogFileReference> iterator = fileStream.iterator();
      while (iterator.hasNext()) {
        LogFileReference item = iterator.next();
        inFlight.addLast(new PendingWork(item, item.getSize(),
            pool.submit(() -> prepare(item, dailyCtx))));
        inFlightBytes += item.getSize();
        while (!inFlight.isEmpty()
            && (inFlight.size() >= prefetch || inFlightBytes > prefetchMaxBytes.toBytes())) {
          PendingWork done = inFlight.pollFirst();
          inFlightBytes -= done.size();
          consume(done, coordinator, processedCount, errorCount);
        }
      }
      while (!inFlight.isEmpty()) {
        PendingWork done = inFlight.pollFirst();
        inFlightBytes -= done.size();
        consume(done, coordinator, processedCount, errorCount);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  private ExecutorService newPool() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  private byte[] downloadBody(LogFileReference itemLog) {
    try (InputStream content = s3Service.getContent(itemLog.getS3Key())) {
      return IOUtils.toByteArray(content);
    } catch (IOException e) {
      throw new UncheckedIOException("download IOException", e);
    }
  }

  private void consume(PendingWork pending, StreamingExportCoordinator coordinator,
      AtomicInteger processedCount, AtomicInteger errorCount) {
    LogFileReference item = pending.ref();
    try {
      pending.work().get().forEach(coordinator::accept);
      processedCount.incrementAndGet();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("processing interrupted", e);
    } catch (ExecutionException e) {
      errorCount.incrementAndGet();
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      log.error("ERRORE: Salto il file {} a causa di: {}", item.getS3Key(), cause.getMessage());
    } catch (Exception e) {
      errorCount.incrementAndGet();
      log.error("ERRORE: Salto il file {} a causa di: {}", item.getS3Key(), e.getMessage());
    }
  }

  private List<ClassifiedLogFragment> prepare(LogFileReference itemLog, DailyContextCfg dailyCtx) {
    LogSaverUtils.initMDC(dailyCtx);
    log.debug("prepare start s3Key={} type={} date={}", itemLog.getS3Key(), itemLog.getType(), dailyCtx.logDate());

    try {
      itemLog.setContent(new ByteArrayInputStream(downloadBody(itemLog)));
      try (Stream<ClassifiedLogFragment> fragments = filter(itemLog, dailyCtx)) {
        return fragments.toList();
      }
    } finally {
      LogSaverUtils.clearMdcFromForkThread();
    }
  }

  private void downloadFilterAccept(LogFileReference itemLog, DailyContextCfg dailyCtx,
      StreamingExportCoordinator coordinator) {
    LogSaverUtils.initMDC(dailyCtx);
    log.debug("downloadFilterAccept start s3Key={} type={} date={}", itemLog.getS3Key(), itemLog.getType(), dailyCtx.logDate());

    try (InputStream content = s3Service.getContent(itemLog.getS3Key());) {
      itemLog.setContent(content);
      try (Stream<ClassifiedLogFragment> fragments = filter(itemLog, dailyCtx)) {
        fragments.forEach(coordinator::accept);
      }

    } catch (IOException e) {
      log.warn("Unexpected error closing input stream");
      throw new UncheckedIOException("downloadFilterAccept IOException", e);
    } finally {
      LogSaverUtils.clearMdcFromForkThread();
    }
  }

  private Stream<ClassifiedLogFragment> filter(LogFileReference itemLog, DailyContextCfg dailyCtx) {
    return itemLog.getType().filter(dailyCtx, itemLog);
  }

}
