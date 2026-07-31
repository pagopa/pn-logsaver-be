package it.pagopa.pn.logsaver.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
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

  @Override
  public List<UploadedPart> process(Stream<LogFileReference> fileStream, DailyContextCfg dailyCtx,
      StreamingExportCoordinator coordinator) {
    log.info("Start processing file - start date={}", dailyCtx.logDate());

    // contatori per i thread
    AtomicInteger processedCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

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
    log.info("Processing completato. Successi: {}, Errori: {}", processedCount.get(), errorCount.get());

    return coordinator.finish();
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
