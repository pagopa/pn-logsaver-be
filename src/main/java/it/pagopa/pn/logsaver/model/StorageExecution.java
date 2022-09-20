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
@Getter
public class StorageExecution {

  private LocalDate latestExecutionDate;

  private List<ItemType> typesProcessed;

  private ExportType exportType;
}
