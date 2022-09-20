package it.pagopa.pn.logsaver.dao.entity;

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

  private String fileName;

  private String storageKey;

  private String result;

  private String tmpPath;

  @Builder
  public AuditStorageEntity(String type, String logDate, String fileName, String storageKey,
      String result, String tmpPath) {
    super(type, logDate);
    this.fileName = fileName;
    this.storageKey = storageKey;
    this.result = result;
    this.tmpPath = tmpPath;
  }

}
