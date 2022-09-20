package it.pagopa.pn.logsaver.services.support;

import java.nio.file.Path;
import java.time.LocalDate;
import it.pagopa.pn.logsaver.model.Retention;

@FunctionalInterface
public interface ExportAudit<F, O, R, D> {


  public void export(Path folder, Path file, Retention retention, LocalDate logDate);
}
