package it.pagopa.pn.logsaver.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.services.s3.model.S3Object;

@Data
@Builder
public class ItemLog {

  private String s3Key;

  private LogType type;

  private String safeStorageKey;

  private LocalDate logDate;

  private LocalDateTime sendDateTime;


  public static ItemLog from(S3Object obj) {
    return ItemLog.builder().s3Key(obj.key()).build();
  }
}
