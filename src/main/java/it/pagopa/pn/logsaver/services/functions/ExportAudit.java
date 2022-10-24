package it.pagopa.pn.logsaver.services.functions;

import java.nio.file.Path;
import java.time.LocalDate;
import it.pagopa.pn.logsaver.model.enums.Retention;

@FunctionalInterface
public interface ExportAudit {
  public void export(Path folder, Path file, Retention retention, LocalDate logDate);
}
