package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  @Default
  private Map<Retention, Set<ExportType>> retentionExportTypeMap = defaultRetentionExportTypeMap();
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

  public boolean retentionHaveExportType(Retention retention) {
    return retentionTmpPath.containsKey(retention);
  }

  private static Map<Retention, Set<ExportType>> defaultRetentionExportTypeMap() {
    return Stream.of(Retention.values())
        .collect(Collectors.toMap(r -> r, r -> Set.of(ExportType.values())));
  }

  public Set<Retention> retentions() {
    return retentionExportTypeMap.keySet();
  }

  public Set<ExportType> exportTypes() {
    return retentionExportTypeMap.values().stream().flatMap(e -> e.stream().distinct()).distinct()
        .collect(Collectors.toSet());
  }

  public Set<ExportType> exportTypes(Retention retention) {
    return retentionExportTypeMap.get(retention);
  }
}
