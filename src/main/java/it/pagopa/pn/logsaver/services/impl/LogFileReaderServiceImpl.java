package it.pagopa.pn.logsaver.services.impl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.S3Object;



@Service
@AllArgsConstructor
@Slf4j
public class LogFileReaderServiceImpl implements LogFileReaderService {

  private final S3BucketClient clientS3;
  private final LogSaverCfg cfg;


  private Stream<String> findSubfolders(LogFileType type, LocalDate logDate) {
    log.info("Start search subfolders for log file {}.", type.name());

    List<String> subFolderListCfg =
        LogFileType.CDC == type ? cfg.getCdcTables() : cfg.getLogsMicroservice();

    if (subFolderListCfg.isEmpty()) {// Ricerca delle subFolders su S3
      return findSubfoldersS3(type, logDate);
    }
    return subFolderListCfg.stream();
  }


  private Stream<String> findSubfoldersS3(LogFileType type, LocalDate logDate) {
    String subFolderFilter = StringUtils.substringBefore(
        LogFileType.CDC == type ? cfg.getCdcRootPathTemplate() : cfg.getLogsRootPathTemplate(), "/")
        .replace("'", "").concat("/");
    List<String> subFolderList = clientS3
        .findSubFolders(subFolderFilter, DateUtils.getYear(logDate)).collect(Collectors.toList());
    if (subFolderList.isEmpty()) {
      return Stream.of("");
    }
    return subFolderList.stream();
  }


  @Override
  public Stream<LogFileReference> findLogFiles(DailyContextCfg dailyCtx) {
    return Stream.of(LogFileType.values())
        .filter(type -> type.containsRetentions(dailyCtx.retentions()))
        .flatMap(type -> findSubfolders(type, dailyCtx.logDate())
            .flatMap(subFolder -> handleLogFileReference(subFolder, type, dailyCtx.logDate())));
  }

  @Override
  public InputStream getContent(String key) {
    return clientS3.getObjectContent(key);
  }


  private Stream<LogFileReference> handleLogFileReference(String subFolder, LogFileType type,
      LocalDate logDate) {

    String prefix = handleDailyPrefix(subFolder, type, logDate);
    log.info("Search {} log files for subfolder {}", type.name(), prefix);
    Stream<S3Object> objList = clientS3.findObjects(prefix);
    return objList.map(
        obj -> LogFileReference.builder().s3Key(obj.key()).type(type).logDate(logDate).build());
  }

  private String handleDailyPrefix(String subFolder, LogFileType type, LocalDate logDate) {
    String dailyTmpPattern =
        LogFileType.CDC == type ? cfg.getCdcRootPathTemplate() : cfg.getLogsRootPathTemplate();
    return String.format(logDate.format(DateTimeFormatter.ofPattern(dailyTmpPattern)), subFolder);
  }


}
