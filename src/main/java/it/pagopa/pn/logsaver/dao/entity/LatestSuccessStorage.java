package it.pagopa.pn.logsaver.dao.entity;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
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
public class LatestSuccessStorage {

  @Getter(onMethod = @__({@DynamoDbPartitionKey}))
  private String type;

  @Getter(onMethod = @__({@DynamoDbSortKey}))
  private String logDate;


  @Getter
  private LocalDate latestSuccessDate;

  @Getter
  private List<String> typesProcessed;


}
