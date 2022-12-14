package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
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

  @Default
  private List<Path> filePath = new ArrayList<>();

  private LocalDate logDate;

  private ExportType exportType;

  public String fileName(Path filePath) {
    return Objects.nonNull(filePath) ? filePath.getFileName().toString() : null;
  }

}
