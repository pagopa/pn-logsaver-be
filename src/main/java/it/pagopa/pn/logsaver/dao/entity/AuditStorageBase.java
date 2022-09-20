package it.pagopa.pn.logsaver.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditStorageBase {

  @Getter(onMethod = @__({@DynamoDbPartitionKey}))
  private String type;

  @Getter(onMethod = @__({@DynamoDbSortKey}))
  private String logDate;



}
