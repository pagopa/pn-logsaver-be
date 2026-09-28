package it.pagopa.pn.logsaver.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.springframework.util.unit.DataSize;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZipExportMultipart extends AbstractExportMultipart<ZipOutputStream> {

  private static final long ZIP_ENTRY_OVERHEAD = 76L;

  private CountingOutputStream countingOut;

  private int entriesInCurrentPart;

  public ZipExportMultipart(@NonNull Path folderIn, @NonNull DataSize maxSize,
      @NonNull Path folderOut, @NonNull String patternFileOut) {
    super(folderIn, maxSize, folderOut, patternFileOut);
  }

  @Override
  protected boolean isPartEmpty() {
    return entriesInCurrentPart == 0;
  }

  @Override
  protected void setCurrentFileOut(Path fileOut) throws IOException {
    this.entriesInCurrentPart = 0;
    OutputStream fileStream =
        Files.newOutputStream(fileOut, StandardOpenOption.APPEND, StandardOpenOption.CREATE_NEW);
    this.countingOut = new CountingOutputStream(fileStream);
    this.currentFileOut = new ZipOutputStream(countingOut);
  }

  @Override
  protected void addLogFile(File filePath) throws IOException {
    ZipEntry ze = new ZipEntry(folderIn.relativize(filePath.toPath()).toString());
    log.info(currentPathFile + "-" + ze.getName());
    currentFileOut.putNextEntry(ze);
    try (FileInputStream fis = new FileInputStream(filePath);) {
      IOUtils.copy(fis, currentFileOut);
      currentFileOut.closeEntry();
      this.entriesInCurrentPart++;
    }
    currentFileOut.flush();
  }

  @Override
  protected void addLogEntry(String entryName, InputStream content) throws IOException {
    byte[] data = IOUtils.toByteArray(content);
    ZipEntry ze = new ZipEntry(entryName);
    log.info(currentPathFile + "-" + ze.getName());
    currentFileOut.putNextEntry(ze);
    currentFileOut.write(data);
    currentFileOut.closeEntry();
    this.entriesInCurrentPart++;
    currentFileOut.flush();
  }

  @Override
  protected void closeCurrentFile() throws IOException {
    currentFileOut.close();

  }

  @Override
  protected long estimatedEntrySize(String entryName, byte[] data) {
    Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
    try {
      deflater.setInput(data);
      deflater.finish();
      byte[] buffer = new byte[8192];
      long compressed = 0L;
      while (!deflater.finished()) {
        compressed += deflater.deflate(buffer);
      }
      return compressed + ZIP_ENTRY_OVERHEAD + 2L * entryName.length();
    } finally {
      deflater.end();
    }
  }

  @Override
  protected long currentPartSize() {
    return countingOut.getByteCount();
  }

}
