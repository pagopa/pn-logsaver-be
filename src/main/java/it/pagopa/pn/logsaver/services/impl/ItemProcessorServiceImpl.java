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
import it.pagopa.pn.logsaver.model.Item;
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

    LocalDateTime start = LocalDateTime.now();
    items.stream().parallel().map(item -> process(item, dailyCtx)).collect(Collectors.toList());
    Duration duration = Duration.between(start, LocalDateTime.now());
    log.trace("Time execution processs: {}", duration.toString());

    start = LocalDateTime.now();
    List<AuditFile> grouped = groupByRetention(dailyCtx);
    duration = Duration.between(start, LocalDateTime.now());
    log.trace("Time execution creation pdf: {}", duration.toString());
    return grouped;

  }



  @Override
  public Item process(Item log, DailyContextCfg dailyCtx) {
    try (InputStream content = s3Service.getItemContent(log.getS3Key());) {
      String fileName = FilenameUtils.getBaseName(log.getS3Key());

      cfg.getFilterFunction(log).apply(content, dailyCtx).forEach(item -> {
        try (InputStream isItem = item.getContent();) {
          writeLog(isItem, fileName, item.getRetention(), dailyCtx);
        } catch (IOException e) {
          // Nothing to do
        }
      });

    } catch (IOException e) {
      // Nothing to do
    }
    return log;
  }


  private void writeLog(InputStream content, String fileName, Retention retention,
      DailyContextCfg dailyCxt) {
    if (Objects.nonNull(retention)) {
      Path path = dailyCxt.getRetentionTmpPath().get(retention);
      FilesUtils.writeFile(content, fileName, path);
    }
  }

  @Override
  public List<AuditFile> groupByRetention(DailyContextCfg dailyCxt) {
    return dailyCxt.getRetentionTmpPath().entrySet().stream()
        .map(entry -> this.createZipFile(entry.getKey(), entry.getValue(), dailyCxt))
        .collect(Collectors.toList());
  }

  private AuditFile createZipFile(Retention retention, Path path, DailyContextCfg dailyCxt) {

    String fileName = dailyCxt.getLogDate()
        .format(DateTimeFormatter.ofPattern(retention.getNameFormat())).concat(".zip");
    Path fileZipOut = Path.of(dailyCxt.getTmpDailyPath().toString(), fileName);

    FilesUtils.zipDirectory(path, fileZipOut);
    return AuditFile.builder().filePath(fileZipOut).logDate(dailyCxt.getLogDate())
        .retention(retention).build();
  }


}
