package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditFile {

  private Retention retention;

  private Path filePath;

  private LocalDate logDate;

}
