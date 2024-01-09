package it.pagopa.pn.logsaver.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.util.unit.DataSize;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZipExportMultipart extends AbstractExportMultipart<ZipOutputStream> {

  public ZipExportMultipart(@NonNull Path folderIn, @NonNull DataSize maxSize,
      @NonNull Path folderOut, @NonNull String patternFileOut) {
    super(folderIn, maxSize, folderOut, patternFileOut);
  }

  @Override
  protected void setCurrentFileOut(Path fileOut) throws IOException {

    this.currentFileOut = new ZipOutputStream(
        Files.newOutputStream(fileOut, StandardOpenOption.APPEND, StandardOpenOption.CREATE_NEW));
  }

  @Override
  protected void addLogFile(File filePath) throws IOException {
    ZipEntry ze = new ZipEntry(folderIn.relativize(filePath.toPath()).toString());
    log.info(currentPathFile + "-" + ze.getName());
    currentFileOut.putNextEntry(ze);
    try (FileInputStream fis = new FileInputStream(filePath)) {
      IOUtils.copy(fis, currentFileOut);
      currentFileOut.closeEntry();
    }
    // delete file added to the ZIP
    if (filePath.exists()) {
      FilesUtils.remove(filePath.toPath());
    }
  }

  @Override
  protected void closeCurrentFile() throws IOException {
    currentFileOut.close();

  }

}
