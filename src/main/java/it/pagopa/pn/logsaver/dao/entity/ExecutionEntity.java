package it.pagopa.pn.logsaver.dao.entity;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Setter
@Getter
@NoArgsConstructor
public class ExecutionEntity extends AuditStorageBase {

  private List<String> itemTypes;

  private Map<String, RetentionResult> retentionResult;

  @Builder
  public ExecutionEntity(String logDate, Map<String, RetentionResult> retentionResult,
      List<String> itemTypes// , List<String> exportTypes, List<String> retentions
  ) {
    super(ExtraType.LOG_SAVER_EXECUTION.name(), logDate);
    this.retentionResult = retentionResult;
    this.itemTypes = itemTypes;
  }
}
