package it.pagopa.pn.logsaver.services.support;

@FunctionalInterface
public interface ExportAudit<F, O, R, D> {
  public void export(F folder, O file, R retention, D logDate);
}
