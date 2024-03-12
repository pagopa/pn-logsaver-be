package it.pagopa.pn.logsaver.services.functions;

import it.pagopa.pn.logsaver.services.FileCompleteListener;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.model.enums.Retention;

//@FunctionalInterface
public interface ExportAudit {

//  public abstract void init(Path folder, Path folderOut, String patternFileName, Retention retention,
//      LocalDate logDate);
  public List<Path> export(Path folderIn, Path folderOut, String patternFileName,
      Retention retention, LocalDate logDate, FileCompleteListener fileCompleteListener);

  public abstract void close();
}
