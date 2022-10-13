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
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ZipUtils {



  public static void createZip(Path dir, Path zipPathout) {
    log.info("Creating zip {} for folder {}", zipPathout.getFileName().toString(), dir.toString());

    try (ZipOutputStream zos = new ZipOutputStream(
        Files.newOutputStream(zipPathout, StandardOpenOption.APPEND, StandardOpenOption.CREATE));) {
      zipDirectory(dir, zos);
    } catch (IOException e) {
      log.error("Error creating zip for folder {}", dir.toString());
      throw new FileSystemException("", e);
    }
  }


  private static void zipDirectory(Path dir, ZipOutputStream zos) throws IOException {

    for (File filePath : dir.toFile().listFiles()) {
      log.trace("Zipping " + filePath.getPath());
      if (filePath.isDirectory()) {
        zipDirectory(filePath.toPath(), zos);
      } else {
        ZipEntry ze = new ZipEntry(filePath.getName());
        zos.putNextEntry(ze);
        try (FileInputStream fis = new FileInputStream(filePath);) {
          IOUtils.copy(fis, zos);
          zos.closeEntry();
        }
      }
    }
  }

}
