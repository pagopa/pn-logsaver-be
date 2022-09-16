package it.pagopa.pn.logsaver.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditStorage {

  private String type;

  private String logDate;

  private String fileName;

  private String storageKey;

  private String result;


  @DynamoDbPartitionKey
  public String getType() {
    return type;
  }

  @DynamoDbSortKey
  public String getLogDate() {
    return logDate;
  }

  public String getFileName() {
    return fileName;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getResult() {
    return result;
  }

}
