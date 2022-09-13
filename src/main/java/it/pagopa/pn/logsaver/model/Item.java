package it.pagopa.pn.logsaver.model;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class Item {

  private String s3Key;

  private ItemType type;

  private String safeStorageKey;

  private LocalDate logDate;

  private LocalDateTime sendDateTime;

  private Stream<ItemChildren> children;

  @AllArgsConstructor
  @Getter
  public static class ItemChildren {

    private Retention retention;

    private InputStream content;

  }

}
