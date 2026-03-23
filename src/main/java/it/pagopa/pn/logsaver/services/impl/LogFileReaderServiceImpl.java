package it.pagopa.pn.logsaver.services.impl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.pagopa.pn.logsaver.dao.StorageDao;
import it.pagopa.pn.logsaver.dao.entity.AuditStorageEntity;
import it.pagopa.pn.logsaver.services.StorageService;
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
  private final StorageDao storageDao;
  private final StorageService storageService;

  private final static String S3_SUBFOLDER_TO_SCAN_NONE = "NONE";
  private final static String S3_SUBFOLDER_TO_SCAN_ALL = "ALL";


  private Stream<String> findSubfolders(LogFileType type, LocalDate logDate) {
    log.info("Start search subfolders for log file {}.", type.name());

    List<String> subFolderListCfg =
        LogFileType.CDC == type ? this.getCdcTables() : cfg.getLogsMicroservice();

    log.info("UAT - findSubfolders type={} date={} configuredList={}", type.name(), logDate, subFolderListCfg);

    if ( LogFileType.LOGS == type ){
      if (subFolderListCfg.isEmpty()) {// Ricerca delle subFolders su S3
        log.info("UAT - findSubfolders type=LOGS configured list is empty, searching subfolders on S3");
        return findSubfoldersS3(type, logDate);
      }
      log.info("UAT - findSubfolders type=LOGS using configured list size={}", subFolderListCfg.size());
      return subFolderListCfg.stream();
    } else {
        if (subFolderListCfg.get(0).equals(S3_SUBFOLDER_TO_SCAN_NONE)) {
          log.info("CDC tables non configurate: nessuna scansione verrà eseguita.");
          log.info("UAT - findSubfolders type=CDC value=NONE, skipping scan, returning empty stream");
          return Stream.empty(); // Non fa nessuna scansione, torna uno stream vuoto
        } else if (subFolderListCfg.get(0).equals(S3_SUBFOLDER_TO_SCAN_ALL)) { // Ricerca delle subFolders su S3
          log.info("UAT - findSubfolders type=CDC value=ALL, searching subfolders on S3");
          return findSubfoldersS3(type, logDate);
        } else {
          log.info("UAT - findSubfolders type=CDC using configured list size={}", subFolderListCfg.size());
          return subFolderListCfg.stream();
        }
    }
  }

  /**
   * Metodo per la ricerca di subFolders su S3. Recupera il pathPrefix del path S3 ed il subFolderPrefix del
   * folder oggetto della ricerca dal file di configurazione.
   * @param type LogFileType: tipo di log (CDC o LOGS)
   * @param logDate LocalDate: data del log
   * @return Stream<String>: stream di subFolders
   */
  private Stream<String> findSubfoldersS3(LogFileType type, LocalDate logDate) {
    /*String subFolderFilter = StringUtils.substringBefore(
        LogFileType.CDC == type ? cfg.getCdcRootPathTemplate() : cfg.getLogsRootPathTemplate(), "/")
        .replace("'", "").concat("/");
    List<String> subFolderList = clientS3
        .findSubFolders(subFolderFilter, DateUtils.getYear(logDate)).collect(Collectors.toList());*/

    // getCdcRootPathTemplate : 'cdcTos3/%s/'yyyy/MM/dd  --> pathPrefix : cdcTos3/
    //                        : 'logsTos3/'yyyy/MM/dd    --> pathPrefix : logsTos3/
	  String pathPrefix = StringUtils.substringBefore(
		        LogFileType.CDC == type ? cfg.getCdcRootPathTemplate() : cfg.getLogsRootPathTemplate(), "/")
		        .replace("'", "").concat("/");

      // getCdcTablesPrefix : TABLE_NAME_
      String subFolderPrefix = LogFileType.CDC == type ? cfg.getCdcTablesPrefix() : "";

    if(LogFileType.CDC == type ) {
      //pathPrefix = pathPrefix.substring(0, pathPrefix.indexOf("/")+1);
      subFolderPrefix = "";
    }
    log.info("UAT - findSubfoldersS3 type={} date={} pathPrefix={} subFolderPrefix={}", type.name(), logDate, pathPrefix, subFolderPrefix);

    List<String> subFolderList = clientS3
            .findSubFoldersWithPrefix(pathPrefix, subFolderPrefix).collect(Collectors.toList());

    log.info("UAT - findSubfoldersS3 type={} date={} subFoldersFound={} list={}", type.name(), logDate, subFolderList.size(), subFolderList);

	  if (subFolderList.isEmpty()) {
        log.warn("UAT - findSubfoldersS3 type={} date={} no subfolders found on S3 for pathPrefix={}, falling back to empty string prefix", type.name(), logDate, pathPrefix);
    	  return Stream.of("");
	  }
	  return subFolderList.stream();
  }


  @Override
  public Stream<LogFileReference> findLogFiles(DailyContextCfg dailyCtx) {
    log.info("UAT - findLogFiles start date={} retentions={} logFileTypes={}", dailyCtx.logDate(), dailyCtx.retentions(), dailyCtx.logFileTypes());

    List<LogFileReference> files = Stream.of(LogFileType.values())
        .filter(type -> type.containsRetentions(dailyCtx.retentions()))
        .flatMap(type -> findSubfolders(type, dailyCtx.logDate())
            .flatMap(subFolder -> handleLogFileReference(subFolder, type, dailyCtx.logDate())))
        .collect(Collectors.toList());

    Map<LogFileType, Long> countByType = files.stream()
        .collect(Collectors.groupingBy(LogFileReference::getType, Collectors.counting()));

    log.info("UAT - findLogFiles end date={} totalFiles={} byType={}", dailyCtx.logDate(), files.size(), countByType);

    if (files.isEmpty()) {
      log.warn("UAT - findLogFiles date={} no files found, check S3 path configuration and subfolder discovery", dailyCtx.logDate());
    }

    return files.stream();
  }

  /**
   * Recupera da DynamoDB tutti i file in stato result e li mappa in uno Stream<LogFileReference>
   * @param result String
   * @return Stream<LogFileReference>
   */
  public List<AuditStorageEntity> findLogFilesByResult(String result) {
    log.info("Invoking findLogFilesByResult() for result: {} ", result);
      return storageService.findAuditStorageByResult(result);
  }


  @Override
  public InputStream getContent(String key) {
    return clientS3.getObjectContent(key);
  }

  private Stream<LogFileReference> handleLogFileReference(String subFolder, LogFileType type,
      LocalDate logDate) {

    if(LogFileType.CDC == type && subFolder.isEmpty())
      return Stream.empty();
    String prefix = handleDailyPrefix(subFolder, type, logDate);
    log.info("Search {} log files for subfolder {}", type.name(), prefix);

    List<S3Object> objList = clientS3.findObjects(prefix).collect(Collectors.toList());
    log.info("UAT - handleLogFileReference type={} prefix={} objectsFound={}", type.name(), prefix, objList.size());

    if (objList.isEmpty()) {
      log.warn("UAT - handleLogFileReference type={} date={} prefix={} no objects found on S3", type.name(), logDate, prefix);
    }

    return objList.stream().map(
        obj -> LogFileReference.builder().s3Key(obj.key()).type(type).logDate(logDate).build());
  }

  private String handleDailyPrefix(String subFolder, LogFileType type, LocalDate logDate) {
    String dailyTmpPattern =
        LogFileType.CDC == type ? cfg.getCdcRootPathTemplate() : cfg.getLogsRootPathTemplate();
    return String.format(logDate.format(DateTimeFormatter.ofPattern(dailyTmpPattern)), subFolder);
  }


  public List<String> getCdcTables() {
      return (cfg.getCdcTables() == null || cfg.getCdcTables().isEmpty()) ? List.of("NONE") : cfg.getCdcTables();
  }

}