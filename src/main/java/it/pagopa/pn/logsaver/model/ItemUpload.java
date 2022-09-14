package it.pagopa.pn.logsaver.model;

import java.nio.file.Path;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(fluent = true)
public class ItemUpload {

  private Retention retention;

  private Path filePath;

  private LocalDate logDate;

  private String econdedSHA256;

  private String uploadKey;


  public static ItemUpload from(ArchiveInfo arc) {
    return ItemUpload.builder().retention(arc.retention()).filePath(arc.filePath())
        .logDate(arc.logDate()).build();
  }
}
