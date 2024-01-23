package it.pagopa.pn.logsaver.services.impl.functions;

import it.pagopa.pn.logsaver.services.FileCompleteListener;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.functions.ExportAudit;
import it.pagopa.pn.logsaver.utils.ZipExportMultipart;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service(ExportType.ZIP_S)
@RequiredArgsConstructor
public class ExportAuditZip implements ExportAudit {

  @NonNull
  private final LogSaverCfg cfg;
  private ZipExportMultipart zipExportMultipart;

  @Override
  public List<Path> export(Path folder, Path folderOut, String patternFileName, Retention retention,
      LocalDate logDate, FileCompleteListener fileCompleteListener) {
    log.info("Creating zip for folder {}", folder.toString());
    return new ZipExportMultipart(folder, cfg.getMaxSize(), folderOut, patternFileName, fileCompleteListener).export();
  }

  public void close(){
    zipExportMultipart.close();
  }

}
