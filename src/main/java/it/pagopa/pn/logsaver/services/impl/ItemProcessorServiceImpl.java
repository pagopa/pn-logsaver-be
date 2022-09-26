package it.pagopa.pn.logsaver.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.exceptions.UncheckedIOException;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.ItemProcessorService;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.utils.FilesUtils;
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
  public List<AuditFile> process(List<Item> items, DailyContextCfg dailyCtx) {
    log.info("Start processing file");
    items.stream().parallel().forEach(item -> process(item, dailyCtx));

    log.info("Start creating files");

    List<AuditFile> groupedAudit = createAuditFile(dailyCtx);
    log.info("Files created {}", groupedAudit.size());
    return groupedAudit;

  }

  private Item process(Item itemLog, DailyContextCfg dailyCtx) {
    log.debug("Dowload file {}", itemLog.getS3Key());
    try (InputStream content = s3Service.getItemContent(itemLog.getS3Key());) {

      String fileName = FilenameUtils.getBaseName(itemLog.getS3Key());
      itemLog.getType().filter(dailyCtx, content)
          .forEach(item -> writeLog(item, fileName, dailyCtx));

    } catch (IOException e) {
      log.warn("Unexpected error closing input stream");
      throw new UncheckedIOException("writeLog IOException", e);
    }
    return itemLog;
  }


  private void writeLog(ItemChildren item, String fileName, DailyContextCfg dailyCxt) {
    try (InputStream isItem = item.getContent();) {
      if (Objects.nonNull(item.getRetention())) {
        Path path = dailyCxt.retentionTmpPath().get(item.getRetention());
        FilesUtils.writeFile(isItem, fileName, path);
      }
    } catch (IOException e) {
      log.warn("Unexpected error closing input stream");
      throw new UncheckedIOException("writeLog IOException", e);
    }
  }

  private List<AuditFile> createAuditFile(DailyContextCfg dailyCxt) {
    return dailyCxt.retentionTmpPath().entrySet().stream()
        .flatMap(entry -> this.createAuditFile(entry.getKey(), entry.getValue(), dailyCxt).stream())
        .collect(Collectors.toList());
  }

  private List<AuditFile> createAuditFile(Retention retention, Path path,
      DailyContextCfg dailyCxt) {

    return dailyCxt.exportTypes(retention).stream().map(exportType -> {
      String fileName =
          dailyCxt.logDate().format(DateTimeFormatter.ofPattern(retention.getNameFormat()))
              .concat(exportType.getExtension());
      Path fileOut = Path.of(dailyCxt.tmpDailyPath().toString(), fileName);

      exportType.write(path, fileOut, retention, dailyCxt.logDate());

      return AuditFile.builder().filePath(fileOut).logDate(dailyCxt.logDate())
          .exportType(exportType).retention(retention).build();
    }).collect(Collectors.toList());

  }

}
