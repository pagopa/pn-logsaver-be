package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;



@Getter
@Accessors(fluent = true)
@Builder
public class DailyContextCfg {

  @NonNull
  private LocalDate logDate;
  @NonNull
  private String tmpBasePath;
  @NonNull
  private Set<ItemType> itemTypes;
  @Default
  private Map<Retention, Path> retentionTmpPath = new LinkedHashMap<>();
  @NonNull
  private Map<Retention, Set<ExportType>> retentionExportTypeMap;

  private Path tmpDailyPath;

  public DailyContextCfg initContext() {
    this.tmpDailyPath = Paths.get(tmpBasePath(), logDate().toString());
    String tmpDailyPathStr = tmpDailyPath.toString();

    retentions().stream().forEach(retention -> retentionTmpPath.computeIfAbsent(retention,
        ret -> Paths.get(tmpDailyPathStr, ret.getFolder())));

    FilesUtils.createOrCleanDirectory(tmpDailyPath());
    FilesUtils.createDirectories(retentionTmpPath().values());
    return this;
  }

  public void destroy() {
    FilesUtils.remove(tmpDailyPath());
  }

  public Set<Retention> retentions() {
    return retentionExportTypeMap.keySet();
  }

  public Set<ExportType> exportTypes(Retention retention) {
    return retentionExportTypeMap.get(retention);
  }
}
