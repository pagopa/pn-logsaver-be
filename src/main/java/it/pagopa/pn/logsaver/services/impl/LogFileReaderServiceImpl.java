package it.pagopa.pn.logsaver.services.impl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileType;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.S3Object;



@Service
@AllArgsConstructor
@Slf4j
public class LogFileReaderServiceImpl implements LogFileReaderService {

  private final S3BucketClient clientS3;
  private final LogSaverCfg cfg;


  private Stream<String> findSubfolders(LogFileType type) {
    log.info("Start search {} log files.", type.name());

    List<String> appsCfg = LogFileType.CDC == type ? cfg.getCdcTables() : cfg.getLogsMicroservice();

    return appsCfg.isEmpty() ? clientS3.findSubFolders(type.getSubFolfer()) : appsCfg.stream();
  }

  @Override
  public Stream<LogFileReference> findLogFiles(DailyContextCfg dailyCtx) {
    return Stream.of(LogFileType.values())
        .filter(type -> type.containsRetentions(dailyCtx.retentions()))
        .flatMap(type -> findSubfolders(type)
            .flatMap(subFolder -> handleLogFileReference(subFolder, type, dailyCtx.logDate())));
  }

  @Override
  public InputStream getContent(String key) {
    return clientS3.getObjectContent(key);
  }


  private Stream<LogFileReference> handleLogFileReference(String appName, LogFileType type,
      LocalDate logDate) {

    String prefix = handleDailyPrefix(appName, type, logDate);
    log.info("Search {} log files for subfolder {}", type.name(), prefix);
    Stream<S3Object> objList = clientS3.findObjects(prefix);
    return objList.map(
        obj -> LogFileReference.builder().s3Key(obj.key()).type(type).logDate(logDate).build());
  }

  private String handleDailyPrefix(String appName, LogFileType type, LocalDate logDate) {
    String dailyTmpPattern =
        LogFileType.CDC == type ? cfg.getCdcRootPathTemplate() : cfg.getLogsRootPathTemplate();
    return String.format(logDate.format(DateTimeFormatter.ofPattern(dailyTmpPattern)), appName);
  }


}
