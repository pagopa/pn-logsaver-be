package it.pagopa.pn.logsaver.services;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.model.ArchiveInfo;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.springbootcfg.DailyContextCfg;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemProcessorServiceImpl {

  @NonNull
  private final LogSaverCfg cfg;
  @NonNull
  private final ItemReaderService s3Service;



  public Item process(Item log, DailyContextCfg dailyCxt) {
    // TODO chiudere gli stream
    InputStream content = s3Service.getItemContent(log.getS3Key());
    String fileName = FilenameUtils.getBaseName(log.getS3Key());

    cfg.getFilterFunction(log).apply(content, dailyCxt).forEach(item -> {
      writeLog(item.getContent(), fileName, item.getRetention(), dailyCxt);
    });
    return log;
  }


  private void writeLog(InputStream content, String fileName, Retention retention,
      DailyContextCfg dailyCxt) {
    if (Objects.nonNull(retention)) {
      Path path = dailyCxt.getRetentionTmpPath().get(retention);
      FilesUtils.writeFile(content, fileName, path);
    }
  }

  public List<ArchiveInfo> zipAllItemsByRetention(DailyContextCfg dailyCxt) {
    return dailyCxt.getRetentionTmpPath().entrySet().stream()
        .map(entry -> this.createZipFile(entry, dailyCxt)).collect(Collectors.toList());
  }

  private ArchiveInfo createZipFile(Entry<Retention, Path> entry, DailyContextCfg dailyCxt) {

    String fileName = dailyCxt.getLogDate()
        .format(DateTimeFormatter.ofPattern(entry.getKey().getZipNameFormat()));
    Path fileZipOut = Path.of(dailyCxt.getTmpDailyPath().toString(), fileName);


    FilesUtils.zipDirectory(entry.getValue(), fileZipOut);
    return ArchiveInfo.builder().filePath(fileZipOut).logDate(dailyCxt.getLogDate())
        .retention(entry.getKey()).build();
  }



}
