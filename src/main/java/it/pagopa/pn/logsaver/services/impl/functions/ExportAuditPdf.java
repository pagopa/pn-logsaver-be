package it.pagopa.pn.logsaver.services.impl.functions;

import it.pagopa.pn.logsaver.services.FileCompleteListener;
import it.pagopa.pn.logsaver.utils.ZipExportMultipart;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.functions.ExportAudit;
import it.pagopa.pn.logsaver.utils.PdfExportMultipart;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service(ExportType.PDF_S)
@RequiredArgsConstructor
public class ExportAuditPdf implements ExportAudit {

  @NonNull
  private final LogSaverCfg cfg;

  @Override
  public List<Path> export(Path folder, Path folderOut, String patternFileName, Retention retention,
      LocalDate logDate, FileCompleteListener fileCompleteListener) {
    log.info("Creating pdf files for folder {}", folder.toString());

    PdfExportMultipart pdfExportMultipart = new PdfExportMultipart(folder, cfg.getMaxSize(), folderOut, patternFileName, retention,
        logDate, fileCompleteListener);
    pdfExportMultipart.init();
    List<Path>  list = pdfExportMultipart.export();
    pdfExportMultipart.close();
    return list;

  }

  @Override
  public void close() {

  }

}
