package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;



@Getter
@Accessors(fluent = true)
@Builder
@Slf4j
public class DailyContextCfg {

  @NonNull
  private LocalDate logDate;
  @NonNull
  private String tmpBasePath;
  @NonNull
  private Set<LogFileType> logFileTypes;
  @Default
  private Map<Retention, Path> retentionTmpFolder = new LinkedHashMap<>();
  @NonNull
  private Map<Retention, Set<ExportType>> retentionExportTypeMap;

  private Path tmpDailyPath;

  public DailyContextCfg initContext() {

    LogSaverUtils.initMDC(this);
    log.info("Init context for day {}", this.logDate());

    this.tmpDailyPath = Paths.get(tmpBasePath(), logDate().toString());
    String tmpDailyPathStr = tmpDailyPath.toString();

    retentions().stream().forEach(retention -> retentionTmpFolder.computeIfAbsent(retention,
        ret -> Paths.get(tmpDailyPathStr, ret.getCode())));

    FilesUtils.createOrCleanDirectory(tmpDailyPath());
    FilesUtils.createDirectories(retentionTmpFolder().values());
    return this;
  }

  public void destroy() {
    FilesUtils.remove(tmpDailyPath());
    MDC.clear();
  }

  public Set<Retention> retentions() {
    return retentionExportTypeMap.keySet();
  }

  public Set<ExportType> getExportTypesByRetention(Retention retention) {
    return retentionExportTypeMap.get(retention);
  }
}
