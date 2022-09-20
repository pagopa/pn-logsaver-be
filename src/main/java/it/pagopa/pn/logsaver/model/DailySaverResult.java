package it.pagopa.pn.logsaver.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailySaverResult {

  private List<AuditFile> auditList;

  private List<AuditStorage> auditUploadList;

  private Boolean success;

  private Throwable error;

}
