package it.pagopa.pn.logsaver.model;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@NoArgsConstructor
@Accessors(fluent = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode()
public class AuditDownloadReference implements ErrorAware {

  private LocalDate logDate;

  private String fileName;

  private String uploadKey;

  private String downloadUrl;

  private BigDecimal size;

  private String checksum;

  private InputStream content;

  private String destinationFolder;

  private Throwable error;

  private AuditStorageStatus status;

}
