package it.pagopa.pn.logsaver.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.Base64Utils;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import it.pagopa.pn.logsaver.model.Retention;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class FilesUtils {

  private static final String START_XML_AUDIT =
      "<audit date=\"%s\" fileName=\"%s\" retention=\"%s\"><![CDATA[";
  private static final byte[] END_XML_AUDIT = "]]</audit>".getBytes();
  private static final int END_XML_AUDIT_SIZE = "]]</audit>".length();


  public static void remove(Path path) {

    try {
      FileUtils.forceDelete(path.toFile());
    } catch (FileNotFoundException | NullPointerException e) {
      // Nothing todo
    } catch (IOException e) {
      throw new FileSystemException("", e);
    }
  }


  public static void createOrCleanDirectory(Path path) {

    try {
      if (Files.notExists(path)) {
        Files.createDirectories(path);
      } else {
        FileUtils.cleanDirectory(path.toFile());
      }

    } catch (IOException e) {

      throw new FileSystemException("", e);
    }
  }


  public static void createDirectories(Collection<Path> pathList) {
    pathList.stream().forEach(FilesUtils::createDirectory);
  }

  public static void createDirectory(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new FileSystemException("", e);
    }

  }



  public static void zipDirectory(Path dir, Path zipPathout) {

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPathout.toFile()));) {

      _zipDirectory(dir, zos);

    } catch (IOException e) {
      throw new FileSystemException("", e);
    }
  }


  private static void _zipDirectory(Path dir, ZipOutputStream zos) {
    try {

      for (File filePath : dir.toFile().listFiles()) {
        log.trace("Zipping " + filePath.getPath());
        if (filePath.isDirectory()) {
          _zipDirectory(filePath.toPath(), zos);
        } else {
          ZipEntry ze = new ZipEntry(filePath.getName());
          zos.putNextEntry(ze);

          FileInputStream fis = new FileInputStream(filePath);
          IOUtils.copy(fis, zos);

          zos.closeEntry();
          fis.close();
        }

      }

    } catch (IOException e) {
      throw new FileSystemException("", e);
    }
  }



  public static void writeXMLFile(InputStream content, String fileName, Path path,
      Retention retention, LocalDate logDate) {
    try {

      Path fullPathFile = Path.of(path.toString(), fileName);
      FileOutputStream fileOut = new FileOutputStream(fullPathFile.toFile(), true);
      if (Files.size(fullPathFile) == 0) {
        InputStream start = new ByteArrayInputStream(
            String.format(START_XML_AUDIT, logDate.toString(), fileName, retention.getNameFormat())
                .getBytes());
        content = new SequenceInputStream(start, content);
      } else {
        FileChannel fl = fileOut.getChannel();
        fl.truncate(fl.size() - END_XML_AUDIT_SIZE);
      }

      IOUtils.copy(new SequenceInputStream(content, new ByteArrayInputStream(END_XML_AUDIT)),
          fileOut);

      fileOut.close();
    } catch (IOException e) {
      log.error("Error writing log file {}", fileName);
      throw new FileSystemException("Error writing xml file", e);
    }

  }



  public static void writeFile(InputStream content, String fileName, Path path) {
    try (FileOutputStream fileOut =
        new FileOutputStream(Paths.get(path.toString(), fileName).toFile(), true);) {
      IOUtils.copy(content, fileOut);
    } catch (IOException e) {
      log.error("Error writing log file {}", fileName);
      throw new FileSystemException("Error writing log file", e);
    }

  }

  public static String computeSha256(byte[] content) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedhash = digest.digest(content);
      return bytesToBase64(encodedhash);
    } catch (NoSuchAlgorithmException exc) {
      throw new InternalException("cannot compute sha256", exc);
    }
  }

  private static String bytesToBase64(byte[] hash) {
    return Base64Utils.encodeToString(hash);
  }


  public static String computeSha256(Path filepath) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      InputStream fileStream = new FileInputStream(filepath.toFile());
      try (DigestInputStream dis = new DigestInputStream(fileStream, md)) {
        while (dis.read() != -1);
        md = dis.getMessageDigest();
      }
      byte[] encodedhash = md.digest();
      return bytesToBase64(encodedhash);
    } catch (NoSuchAlgorithmException | IOException exc) {
      throw new InternalException("cannot compute sha256", exc);
    }
  }
}
