package it.pagopa.pn.logsaver.dao.entity;

import java.time.LocalDate;
import it.pagopa.pn.logsaver.dao.StorageDao;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Setter
@Getter
@NoArgsConstructor
public class ContinuosExecutionEntity extends AuditStorageBase {
  public static final String FIELD_EXECUTION_DATE = "latestExecutionDate";

  private LocalDate latestExecutionDate;

  public ContinuosExecutionEntity(LocalDate latestExecutionDate) {
    super(ExtraType.CONTINUOS_EXECUTION.name(), StorageDao.FIRST_START_DAY);
    this.latestExecutionDate = latestExecutionDate;
  }

}
