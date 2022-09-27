package it.pagopa.pn.logsaver.model;

import java.io.InputStream;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class Item {

  private String s3Key;

  private ItemType type;

  private LocalDate logDate;


  @AllArgsConstructor
  @Getter
  public static class ItemChildren {

    private Retention retention;

    private InputStream content;

  }

}
