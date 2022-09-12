package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArchiveInfo {

  private Retention retention;

  private Path filePath;

  private LocalDate logDate;
}
