package it.pagopa.pn.logsaver.model;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StorageExecution {

  @Getter
  private LocalDate latestExecutionDate;

  @Getter
  private List<ItemType> typesProcessed;


}
