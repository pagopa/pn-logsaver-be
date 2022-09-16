package it.pagopa.pn.logsaver.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailySaverResult {

  public enum DailySaverStatus {
    GENERATED, SENDED
  };

  private List<AuditContainer> auditList;

  private List<ItemUpload> auditUploadList;
  // private Map<LocalDate, List<ItemType>>;

  private DailySaverStatus result;

  private Throwable error;

}
