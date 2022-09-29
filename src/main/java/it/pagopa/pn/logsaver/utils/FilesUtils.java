package it.pagopa.pn.logsaver.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.Base64Utils;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class FilesUtils {

  public static void remove(Path path) {

    try {
      FileUtils.forceDelete(path.toFile());
    } catch (FileNotFoundException | NullPointerException e) {
      // Nothing to do
    } catch (IOException e) {
      log.error("Error removing folder {}", path.toString());
      throw new FileSystemException("Error removing folder", e);
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
      log.error("Error creating or cleaning folder {}", path.toString());
      throw new FileSystemException("Error creating or cleaning folder", e);
    }
  }

  public static void createDirectories(Collection<Path> pathList) {
    pathList.stream().forEach(FilesUtils::createDirectory);
  }

  public static void createDirectory(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      log.error("Error creating folder {}", path.toString());
      throw new FileSystemException("Error creating folder", e);
    }

  }

  public static void writeFile(InputStream content, String fileName, Path path) {
    try (OutputStream fileOut = Files.newOutputStream(Paths.get(path.toString(), fileName),
        StandardOpenOption.APPEND, StandardOpenOption.CREATE);) {
      IOUtils.copy(content, fileOut);
    } catch (IOException e) {
      log.error("Error writing log file {}", fileName);
      throw new FileSystemException("Error writing log file", e);
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
      throw new InternalException("Cannot compute sha256", exc);
    }
  }
}
