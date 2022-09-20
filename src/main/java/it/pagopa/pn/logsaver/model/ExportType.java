package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.time.LocalDate;
import it.pagopa.pn.logsaver.services.support.ExportAudit;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import it.pagopa.pn.logsaver.utils.PdfUtils;
import lombok.Getter;


public enum ExportType {
  ZIP(".zip", "application/zip",
      (exportDir, out, retention, logDate) -> FilesUtils.zipDirectory(exportDir, out)), PDF(".pdf",
          "application/pdf", (exportDir, out, retention, logDate) -> PdfUtils.createPdf(exportDir,
              out, retention, logDate)),;

  @Getter
  private String extension;

  private ExportAudit<Path, Path, Retention, LocalDate> exportWriter;

  @Getter
  private String mediaType;

  ExportType(String extension, String mediaType,
      ExportAudit<Path, Path, Retention, LocalDate> exportWriter) {
    this.extension = extension;
    this.exportWriter = exportWriter;
    this.mediaType = mediaType;
  }

  public void write(Path folder, Path file, Retention retention, LocalDate logDate) {
    this.exportWriter.export(folder, file, retention, logDate);
  }

}
