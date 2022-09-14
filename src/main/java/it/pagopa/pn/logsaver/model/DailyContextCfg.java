package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor(staticName = "of")
@Getter
public class DailyContextCfg {

  @NonNull
  private LocalDate logDate;
  @NonNull
  private String tmpBasePath;

  private Map<Retention, Path> retentionTmpPath = new LinkedHashMap<>();

  private Path tmpDailyPath;


  public DailyContextCfg initContext() {
    this.tmpDailyPath = Paths.get(getTmpBasePath(), getLogDate().toString());
    String tmpDailyPathStr = tmpDailyPath.toString();


    Stream.of(Retention.values()).forEach(retention -> {
      retentionTmpPath.computeIfAbsent(retention,
          ret -> Paths.get(tmpDailyPathStr, ret.getFolder()));
    });

    FilesUtils.createOrCleanDirectory(getTmpDailyPath());
    FilesUtils.createDirectories(getRetentionTmpPath().values());
    return this;
  }

  public void destroy() {
    FilesUtils.remove(getTmpDailyPath());
  }

}
