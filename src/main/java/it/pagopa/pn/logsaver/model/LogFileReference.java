package it.pagopa.pn.logsaver.model;

import java.io.InputStream;
import java.time.LocalDate;
import org.apache.commons.io.FilenameUtils;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class LogFileReference {

  private String s3Key;

  private LogFileType type;

  private LocalDate logDate;

  private InputStream content;

  public String getFileName() {
    return FilenameUtils.getBaseName(this.getS3Key());
  }


  @AllArgsConstructor
  @Getter
  public static class ClassifiedLogFragment {

    private Retention retention;

    private InputStream content;

    private String fileName;
  }

}
