package it.pagopa.pn.logsaver.model;

import java.io.InputStream;
import java.math.BigDecimal;
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
@EqualsAndHashCode(callSuper = true)
public class AuditStorageReference extends AuditStorage {

  private String fileName;

  private String downloadUrl;

  private BigDecimal size;

  private String checksum;

  private InputStream content;

  private String destinationFolder;

}
