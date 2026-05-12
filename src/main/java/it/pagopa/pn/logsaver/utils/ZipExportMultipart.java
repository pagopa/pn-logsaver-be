package it.pagopa.pn.logsaver.utils;

import java.io.ByteArrayOutputStream;
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

  private long currentCompressedSize = 0;

  public ZipExportMultipart(@NonNull Path folderIn, @NonNull DataSize maxSize,
      @NonNull Path folderOut, @NonNull String patternFileOut) {
    super(folderIn, maxSize, folderOut, patternFileOut);
  }

  @Override
  protected void setCurrentFileOut(Path fileOut) throws IOException {
    this.currentCompressedSize = 0;
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
      currentCompressedSize += ze.getCompressedSize();
    }
  }

  @Override
  protected long fileSize(Path pathFile, File nextFile) throws IOException {
    if (currentCompressedSize == 0) {
      return 0;
    }
    return currentCompressedSize + estimateCompressedSize(nextFile);
  }

  @Override
  protected void closeCurrentFile() throws IOException {
    currentFileOut.close();
  }

  private long estimateCompressedSize(File file) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos);
        FileInputStream fis = new FileInputStream(file)) {
      zos.putNextEntry(new ZipEntry(file.getName()));
      IOUtils.copy(fis, zos);
      zos.closeEntry();
    }
    return baos.size();
  }

}
