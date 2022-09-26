package it.pagopa.pn.logsaver.utils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.SequenceInputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Collection;
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
      // Nothing to do
    } catch (IOException e) {
      log.error("Error removing folder {}", path.toString());
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
      log.error("Error creating or cleaning folder {}", path.toString());
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
      log.error("Error creating folder {}", path.toString());
      throw new FileSystemException("Error creating folder", e);
    }

  }

  public static void writeXMLFile(InputStream content, String fileName, Path path,
      Retention retention, LocalDate logDate) {

    Path fullPathFile = Path.of(path.toString(), fileName);

    try (RandomAccessFile writer = new RandomAccessFile(fullPathFile.toString(), "rw");
        FileChannel fl = writer.getChannel();) {

      if (Files.size(fullPathFile) == 0) {
        InputStream start = new ByteArrayInputStream(
            String.format(START_XML_AUDIT, logDate.toString(), fileName, retention.getNameFormat())
                .getBytes());
        content = new SequenceInputStream(start, content);
      } else {
        fl.truncate(fl.size() - END_XML_AUDIT_SIZE);
      }

      ReadableByteChannel channelIn = Channels
          .newChannel(new SequenceInputStream(content, new ByteArrayInputStream(END_XML_AUDIT)));
      long chunk = 1024 * 1024L;
      long cnt;
      for (long offset = 0; (cnt = fl.transferFrom(channelIn, offset, chunk)) > 0; offset += cnt);

    } catch (IOException e) {
      log.error("Error writing xml log file {}", fileName);
      throw new FileSystemException("Error writing xml file", e);
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
