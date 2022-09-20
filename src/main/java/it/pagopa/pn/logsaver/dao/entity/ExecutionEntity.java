package it.pagopa.pn.logsaver.dao.entity;

import java.time.LocalDate;
import java.util.List;
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

  private LocalDate latestExecutionDate;

  private List<String> typesProcessed;

  private String exportType;

  @Builder
  public ExecutionEntity(String type, String logDate, LocalDate latestExecutionDate,
      List<String> typesProcessed, String exportType) {
    super(type, logDate);
    this.latestExecutionDate = latestExecutionDate;
    this.typesProcessed = typesProcessed;
    this.exportType = exportType;
  }

}
