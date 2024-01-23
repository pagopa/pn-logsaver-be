package it.pagopa.pn.logsaver.utils;

import it.pagopa.pn.logsaver.services.FileCompleteListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.IOUtils;
import org.springframework.util.unit.DataSize;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZipExportMultipart extends AbstractExportMultipart<ZipFile> {

  public ZipExportMultipart(@NonNull Path folderIn, @NonNull DataSize maxSize,
      @NonNull Path folderOut, @NonNull String patternFileOut, FileCompleteListener fileCompleteListener) {
    super(folderIn, maxSize, folderOut, patternFileOut, fileCompleteListener);
    this.compressionFactor = 0.3;
  }


  @Override
  protected void setCurrentFileOut(Path fileOut) throws IOException {
    this.currentFileOut = new ZipFile(fileOut.toFile());
  }

  @Override
  protected void addLogFile(File filePath) throws IOException {
    this.currentFileOut.addFile(filePath);
    //NO perché poi il pdf non lo trova!
//    // delete file added to the ZIP
//    if (filePath.exists()) {
//      FilesUtils.remove(filePath.toPath());
//    }
  }

  @Override
  protected void closeCurrentFile() throws IOException {
    log.error("----------------------------------------------");
    log.error(" closing {}", currentPathFile.getFileName().toString());
    log.error("----------------------------------------------");

    currentFileOut.close();
  }

}
