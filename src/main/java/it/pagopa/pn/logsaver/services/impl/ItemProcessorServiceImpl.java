package it.pagopa.pn.logsaver.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.ItemProcessorService;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemProcessorServiceImpl implements ItemProcessorService {


  @NonNull
  private final ItemReaderService s3Service;


  @Override
  public List<AuditFile> process(Stream<Item> itemStream, DailyContextCfg dailyCtx) {
    // Riduzione consapevole. Sono stati fatti dei test in locale e la riduzione migliora
    // notevolmente in tempi di esecuzione.
    List<Item> items = itemStream.collect(Collectors.toList());
    log.info("Total items {}", items.size());
    log.info("Start processing file");
    LogSaverUtils.toParallelStream(items).forEach(item -> downloadFilterWrite(item, dailyCtx));

    log.info("Start creating files");
    List<AuditFile> groupedAudit = createAuditFile(dailyCtx);
    log.info("Files created {}", groupedAudit.size());
    return groupedAudit;

  }

  private void downloadFilterWrite(Item itemLog, DailyContextCfg dailyCtx) {
    log.debug("Dowload file {}", itemLog.getS3Key());
    // Download file dal bucket
    try (InputStream content = s3Service.getItemContent(itemLog.getS3Key());) {
      itemLog.setContent(content);
      // Raggruppo il contenuto del file per Retention
      filter(itemLog, dailyCtx)
          // Scrivo in cartella temporanea
          .forEach(audit -> writeLog(audit, dailyCtx));

    } catch (IOException e) {
      log.warn("Unexpected error closing input stream");
      throw new UncheckedIOException("writeLog IOException", e);
    }
  }

  private Stream<ItemChildren> filter(Item itemLog, DailyContextCfg dailyCtx) {
    return itemLog.getType().filter(dailyCtx, itemLog);
  }

  private void writeLog(ItemChildren audit, DailyContextCfg dailyCxt) {
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
    return dailyCxt.retentionTmpFolder().entrySet()// Per ogni Retention
        .stream()// Creo uno o più file
        .flatMap(
            entry -> this.createAuditFileForRetention(entry.getKey(), entry.getValue(), dailyCxt))
        .collect(Collectors.toList());
  }

  private Stream<AuditFile> createAuditFileForRetention(Retention retention, Path inputFolder,
      DailyContextCfg dailyCxt) {

    return dailyCxt.getExportTypesByRetention(retention)// Ricavo le tipologie di export
        .stream().map(exportType -> { // Per ogni tipologia di export
          // Creo il file
          Path fileOut = handleAuditFilePath(retention, exportType, dailyCxt);
          exportType.write(inputFolder, fileOut, retention, dailyCxt.logDate());

          return AuditFile.builder().filePath(fileOut).logDate(dailyCxt.logDate())
              .exportType(exportType).retention(retention).build();
        });
  }

  private Path handleAuditFilePath(Retention retention, ExportType exportType,
      DailyContextCfg dailyCxt) {
    String fileName =
        dailyCxt.logDate().format(DateTimeFormatter.ofPattern(retention.getFileNamePattern()))
            .concat(exportType.getExtension());
    return Path.of(dailyCxt.tmpDailyPath().toString(), fileName);
  }

}
