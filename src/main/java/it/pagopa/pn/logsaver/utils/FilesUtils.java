package it.pagopa.pn.logsaver.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.Base64Utils;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import it.pagopa.pn.logsaver.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilesUtils {


  public static void remove(Path path) {

    try {
      FileUtils.forceDelete(path.toFile());
    } catch (FileNotFoundException | NullPointerException e) {
      // Nothing todo
    } catch (IOException e) {
      throw new RuntimeException();
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

      throw new RuntimeException();
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

  public static byte[] decompressGzipToBytes(InputStream zip) {

    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      decompressGzipToBytes(zip, output);

      return output.toByteArray();
    } catch (IOException e) {
      throw new FileSystemException("", e);
    }

  }


  public static void decompressGzipToBytes(InputStream in, OutputStream output) {

    try (GZIPInputStream gis = new GZIPInputStream(in)) {
      IOUtils.copy(gis, output);
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
        log.info("Zipping " + filePath.getPath());
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

  /*
   * 
   * private static void compressDirectoryToZipfile(String rootDir, String sourceDir,
   * ZipOutputStream out) throws IOException, FileNotFoundException { String dir =
   * Paths.get(rootDir, sourceDir).toString(); for (File file : new File(dir).listFiles()) { if
   * (file.isDirectory()) { compressDirectoryToZipfile(rootDir,
   * Paths.get(sourceDir,file.getName()).toString(), out); } else { ZipEntry entry = new
   * ZipEntry(Paths.get(sourceDir,file.getName()).toString()); out.putNextEntry(entry);
   * 
   * FileInputStream in = new FileInputStream(Paths.get(rootDir, sourceDir,
   * file.getName()).toString()); IOUtils.copy(in, out); IOUtils.closeQuietly(in); } } }
   */


  public static void writeFile(byte[] content, String fileName, Path path) {
    try {

      Files.write(Paths.get(path.toString(), fileName), content, StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);

    } catch (IOException e) {

      throw new FileSystemException("", e);
    }

  }


  public static void writeFile(InputStream content, String fileName, Path path) {
    try {
      IOUtils.copy(content,
          new FileOutputStream(Paths.get(path.toString(), fileName).toFile(), true));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public static String computeSha256(byte[] content) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedhash = digest.digest(content);
      return bytesToBase64(encodedhash);
    } catch (NoSuchAlgorithmException exc) {
      throw new PnInternalException("cannot compute sha256", exc);
    }
  }

  private static String bytesToBase64(byte[] hash) {
    return Base64Utils.encodeToString(hash);
  }

  public static byte[] fileToByteArray(Path file) {
    try {
      return Files.readAllBytes(file);
    } catch (IOException e) {
      throw new FileSystemException("", e);
    }
  }
}
