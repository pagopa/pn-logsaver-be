package it.pagopa.pn.logsaver.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Setter
@Getter
@AllArgsConstructor
@DynamoDbBean
@NoArgsConstructor
public class RetentionResult {
  private String retention;
  private String result;
  private String exportType;

  public String getKey() {
    return String.join(ExecutionEntity.KEY_SEPARATOR, getRetention(), getExportType());
  }
}
