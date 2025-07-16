package it.pagopa.pn.logsaver.dao.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Setter
@Getter
@NoArgsConstructor
@ToString
public class ExecutionEntity extends AuditStorageBase {

  private List<String> logFileTypes;

  private Map<String, RetentionResult> retentionResult;

  private BigDecimal expiration;

  @Builder
  public ExecutionEntity(String logDate, Map<String, RetentionResult> retentionResult,
      List<String> logFileTypes, BigDecimal expiration) {
    super(ExtraType.LOG_SAVER_EXECUTION.name(), logDate);
    this.retentionResult = retentionResult;
    this.logFileTypes = logFileTypes;
    this.expiration = expiration;
  }
}
