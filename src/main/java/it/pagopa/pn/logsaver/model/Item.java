package it.pagopa.pn.logsaver.model;

import java.io.InputStream;
import java.time.LocalDate;
import org.apache.commons.io.FilenameUtils;
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

  private InputStream content;

  public String getFileName() {
    return FilenameUtils.getBaseName(this.getS3Key());
  }


  @AllArgsConstructor
  @Getter
  public static class ItemChildren {

    private Retention retention;

    private InputStream content;

    private String fileName;
  }

}
