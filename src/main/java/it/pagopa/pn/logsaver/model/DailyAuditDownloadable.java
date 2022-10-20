package it.pagopa.pn.logsaver.model;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;


/**
 * Rappresenta l'elenco dei riferimenti per il downaload dei file audit di una determinata data
 *
 */
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Accessors(fluent = true)
public class DailyAuditDownloadable {
  @NonNull
  private LocalDate logDate;
  @NonNull
  private List<AuditDownloadReference> audits;
}
