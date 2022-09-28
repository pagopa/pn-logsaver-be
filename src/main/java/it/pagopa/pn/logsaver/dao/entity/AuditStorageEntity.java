package it.pagopa.pn.logsaver.dao.entity;

import org.apache.commons.lang3.StringUtils;
import it.pagopa.pn.logsaver.model.ExportType;
import it.pagopa.pn.logsaver.model.Retention;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Setter
@Getter
@NoArgsConstructor
public class AuditStorageEntity extends AuditStorageBase {

  private String retentionType;

  private String contentType;

  private String fileName;

  private String storageKey;

  private String result;


  @Builder
  public AuditStorageEntity(Retention retention, ExportType exportType, String logDate,
      String fileName, String storageKey, String result) {
    super(handlePKey(retention, exportType), logDate);
    this.fileName = fileName;
    this.storageKey = storageKey;
    this.result = result;
    this.retentionType = retention.name();
    this.contentType = exportType.name();

  }

  private static String handlePKey(Retention retention, ExportType expType) {
    return String.join(KEY_SEPARATOR, retention.name(), expType.name());
  }

  public String getExportType() {
    return StringUtils.substringAfter(this.getType(), KEY_SEPARATOR);
  }

  public String getRetention() {
    return StringUtils.substringBefore(this.getType(), KEY_SEPARATOR);
  }
}
