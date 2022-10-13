package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.logsaver.services.functions.ExportAudit;
import it.pagopa.pn.logsaver.services.impl.functions.ExportAuditPdf;
import it.pagopa.pn.logsaver.utils.ZipUtils;
import lombok.Getter;


public enum ExportType {
  ZIP(".zip", "application/zip",
      (exportDir, out, retention, logDate) -> ZipUtils.createZip(exportDir, out)), PDF_SIGNED(
          ".pdf", "application/pdf", new ExportAuditPdf());

  @Getter
  private String extension;

  private ExportAudit exportWriter;

  @Getter
  private String mediaType;

  ExportType(String extension, String mediaType, ExportAudit exportWriter) {
    this.extension = extension;
    this.exportWriter = exportWriter;
    this.mediaType = mediaType;
  }

  public void write(Path folder, Path file, Retention retention, LocalDate logDate) {
    this.exportWriter.export(folder, file, retention, logDate);
  }

  public static List<String> valuesAsString() {
    return IEnum.valuesAsString(ExportType.class);
  }

  public static List<String> valuesAsString(Collection<ExportType> list) {
    return IEnum.valuesAsString(list);
  }

  public static Set<ExportType> values(List<String> list) {
    return IEnum.values(list, ExportType.class);
  }
}
