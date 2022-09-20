package it.pagopa.pn.logsaver.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.ItemProcessorService;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemProcessorServiceImpl implements ItemProcessorService {

  @NonNull
  private final LogSaverCfg cfg;
  @NonNull
  private final ItemReaderService s3Service;


  @Override
  public List<AuditFile> process(List<Item> items, DailyContextCfg dailyCtx) {
    log.info("Start process files");
    LocalDateTime start = LocalDateTime.now();
    items.stream().parallel().map(item -> process(item, dailyCtx)).collect(Collectors.toList());
    Duration duration = Duration.between(start, LocalDateTime.now());
    log.debug("Execution time process: {}", duration.toString());
    log.info("Start grouping by retention");
    start = LocalDateTime.now();
    List<AuditFile> groupedAudit = groupByRetention(dailyCtx);
    duration = Duration.between(start, LocalDateTime.now());
    log.debug("Execution time creation pdf: {}", duration.toString());
    return groupedAudit;

  }



  private Item process(Item itemLog, DailyContextCfg dailyCtx) {
    log.debug("Begin process file {}", itemLog.getS3Key());
    try (InputStream content = s3Service.getItemContent(itemLog.getS3Key());) {

      String fileName = FilenameUtils.getBaseName(itemLog.getS3Key());
      cfg.filter(itemLog.getType(), content, dailyCtx)
          .forEach(item -> writeLog(item, fileName, dailyCtx));

    } catch (IOException e) {
      log.warn("Unexpected error closing input stream");
    }
    return itemLog;
  }


  private void writeLog(ItemChildren item, String fileName, DailyContextCfg dailyCxt) {
    try (InputStream isItem = item.getContent();) {
      if (Objects.nonNull(item.getRetention())) {
        Path path = dailyCxt.getRetentionTmpPath().get(item.getRetention());
        FilesUtils.writeFile(isItem, fileName, path);
      }
    } catch (IOException e) {
      log.warn("Unexpected error closing input stream");
    }

  }

  @Override
  public List<AuditFile> groupByRetention(DailyContextCfg dailyCxt) {
    return dailyCxt.getRetentionTmpPath().entrySet().stream()
        .map(entry -> this.createAuditFile(entry.getKey(), entry.getValue(), dailyCxt))
        .collect(Collectors.toList());
  }

  private AuditFile createAuditFile(Retention retention, Path path, DailyContextCfg dailyCxt) {
    ExportType exportType = dailyCxt.getExportType();
    String fileName =
        dailyCxt.getLogDate().format(DateTimeFormatter.ofPattern(retention.getNameFormat()))
            .concat(exportType.getExtension());
    Path fileOut = Path.of(dailyCxt.getTmpDailyPath().toString(), fileName);

    exportType.write(path, fileOut, retention, dailyCxt.getLogDate());

    return AuditFile.builder().filePath(fileOut).logDate(dailyCxt.getLogDate())
        .exportType(exportType).retention(retention).build();
  }

}
