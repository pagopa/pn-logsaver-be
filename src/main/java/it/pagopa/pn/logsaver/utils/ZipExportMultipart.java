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
  protected ZipOutputStream newOutputStream(Path fileOut) throws IOException {

    return new ZipOutputStream(
        Files.newOutputStream(fileOut, StandardOpenOption.APPEND, StandardOpenOption.CREATE_NEW));
  }

  @Override
  protected void addFile(File filePath) throws IOException {
    ZipEntry ze = new ZipEntry(folderIn.relativize(filePath.toPath()).toString());
    log.info(currentPathFile + "-" + ze.getName());
    currentFile.putNextEntry(ze);
    try (FileInputStream fis = new FileInputStream(filePath);) {
      IOUtils.copy(fis, currentFile);
      currentFile.closeEntry();
    }
    // zos.flush();
  }

  @Override
  protected void closeCurrentFile() throws IOException {
    currentFile.close();

  }

}
