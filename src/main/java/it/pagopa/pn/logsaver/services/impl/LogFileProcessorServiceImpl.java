package it.pagopa.pn.logsaver.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.LogFileProcessorService;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.services.functions.ExportAudit;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogFileProcessorServiceImpl implements LogFileProcessorService {


  @NonNull
  private final LogFileReaderService s3Service;
  @NonNull
  private final Map<String, ExportAudit> exportFactory;

  @Override
  public List<AuditFile> process(Stream<LogFileReference> fileStream, DailyContextCfg dailyCtx) {
    // Riduzione consapevole. Sono stati fatti dei test in locale e la riduzione migliora
    // notevolmente in tempi di esecuzione.
    List<LogFileReference> fileList = fileStream.collect(Collectors.toList());
    log.info("Total files {}", fileList.size());
    if (fileList.isEmpty()) {
      log.warn("process date={} file list is empty, no files will be processed", dailyCtx.logDate());
    }

    log.info("Start processing file");

    // contatori per i thread
    AtomicInteger processedCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

    LogSaverUtils.toParallelStream(fileList).forEach(item -> //downloadFilterWrite(item, dailyCtx)
            {
              try {
                downloadFilterWrite(item, dailyCtx);
                processedCount.incrementAndGet();
              } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("ERRORE: Salto il file {} a causa di: {}", item.getS3Key(), e.getMessage());
                throw e;
              }
            }
    );
    log.info("Processing completato. Successi: {}, Errori: {}", processedCount.get(), errorCount.get());
    if (processedCount.get() + errorCount.get() < fileList.size()) {
      log.warn("ATTENZIONE: Alcuni file sono andati perduti nel parallelStream! ({} file non pervenuti)",
              fileList.size() - (processedCount.get() + errorCount.get()));
    }

    log.info("Start creating files");
    List<AuditFile> groupedAudit = createAuditFile(dailyCtx);
    log.info("Files created {} - process end date={}", groupedAudit.size(), dailyCtx.logDate());

    return groupedAudit;

  }

  private void downloadFilterWrite(LogFileReference itemLog, DailyContextCfg dailyCtx) {
    LogSaverUtils.initMDC(dailyCtx);
    log.debug("downloadFilterWrite start - Dowload file s3Key={} type={} date={}", itemLog.getS3Key(), itemLog.getType(), dailyCtx.logDate());
    // Download file dal bucket
    try (InputStream content = s3Service.getContent(itemLog.getS3Key());) {
      itemLog.setContent(content);
      // Raggruppo il contenuto del file per Retention
      filter(itemLog, dailyCtx)
          // Scrivo in cartella temporanea
          .forEach(audit -> writeLog(audit, dailyCtx));

    } catch (IOException e) {
      log.warn("Unexpected error closing input stream");
      throw new UncheckedIOException("writeLog IOException", e);
    } finally {
      LogSaverUtils.clearMdcFromForkThread();
    }
  }

  private Stream<ClassifiedLogFragment> filter(LogFileReference itemLog, DailyContextCfg dailyCtx) {
    return itemLog.getType().filter(dailyCtx, itemLog);
  }

  private void writeLog(ClassifiedLogFragment audit, DailyContextCfg dailyCxt) {
    try (InputStream isItem = audit.getContent();) {
      if (Objects.nonNull(audit.getRetention())) {
        Path path = dailyCxt.retentionTmpFolder().get(audit.getRetention());
        FilesUtils.writeFile(isItem, audit.getFileName(), path);
      }
    } catch (IOException e) {
      log.warn("Unexpected error closing input stream");
      throw new UncheckedIOException("writeLog IOException", e);
    }
  }

  private List<AuditFile> createAuditFile(DailyContextCfg dailyCxt) {
    log.info("Start createAuditFile() with dailyCxt {}", dailyCxt);
    return dailyCxt.retentionTmpFolder().entrySet()// Per ogni Retention
        .stream()// Creo uno o più file
        .flatMap(
            entry -> this.createAuditFileForRetention(entry.getKey(), entry.getValue(), dailyCxt))
        .collect(Collectors.toList());
  }

  private Stream<AuditFile> createAuditFileForRetention(Retention retention, Path inputFolder,
      DailyContextCfg dailyCxt) {
    log.info("Start createAuditFileForRetention with retention {}, inputFolder {}, dailyCxt {}", retention.name(),inputFolder.getFileName(), dailyCxt);

    return dailyCxt.getExportTypesByRetention(retention)// Ricavo le tipologie di export
        .stream().map(exportType -> { // Per ogni tipologia di export
          // Creo il file
          String fileNamePattern = handleAuditFileNamePattern(retention, exportType, dailyCxt);

          List<Path> exportParts = exportFactory.get(exportType.getName()).export(inputFolder,
              dailyCxt.tmpDailyPath(), fileNamePattern, retention, dailyCxt.logDate());

          return AuditFile.builder().filePath(exportParts).logDate(dailyCxt.logDate())
              .exportType(exportType).retention(retention).build();
        });
  }

  private String handleAuditFileNamePattern(Retention retention, ExportType exportType,
      DailyContextCfg dailyCxt) {
    log.info("Start handleAuditFileNamePattern");
    String resultString = dailyCxt.logDate().format(DateTimeFormatter.ofPattern(retention.getFileNamePattern()))
            .concat(exportType.getExtension());
    log.info("End handleAuditFileNamePattern with resultString {}", resultString);
    return resultString;
  }

}
