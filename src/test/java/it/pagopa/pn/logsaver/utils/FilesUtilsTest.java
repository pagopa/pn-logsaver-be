package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import it.pagopa.pn.logsaver.utils.FilesUtils;


class FilesUtilsTest {



  @Test
  void remove_WhenFileNotFound_ThenQuietly() {
    FilesUtils.remove(Path.of("abcdegfdgfdafaf"));
    assertTrue(true);
  }


  @Test
  void remove_WhenIOException_ThenThrowFileSystemException() {

    try (MockedStatic<FileUtils> mock = Mockito.mockStatic(FileUtils.class);) {
      mock.when(() -> FileUtils.forceDelete(any())).thenThrow(IOException.class);
      Path path = Path.of("tmp/ddddasd");
      assertThrows(FileSystemException.class, () -> FilesUtils.remove(path));

    }
  }

  @Test
  void createOrCleanDirectory_WhenIOException_ThenThrowFileSystemException() {

    try (MockedStatic<Files> mock = Mockito.mockStatic(Files.class);) {
      mock.when(() -> Files.createDirectories(any())).thenThrow(IOException.class);
      Path path = Path.of("tmp/ddddasd/");
      assertThrows(FileSystemException.class, () -> FilesUtils.createOrCleanDirectory(path));
    }
  }


  @Test
  void createDirectory_WhenIOException_ThenThrowFileSystemException() {

    try (MockedStatic<Files> mock = Mockito.mockStatic(Files.class);) {
      mock.when(() -> Files.createDirectories(any())).thenThrow(IOException.class);
      Path path = Path.of("tmp/ddddasd/");
      assertThrows(FileSystemException.class, () -> FilesUtils.createDirectory(path));
    }
  }


  @Test
  void writeFile_WhenIOException_ThenThrowFileSystemException() {

    try (MockedStatic<Files> mock = Mockito.mockStatic(Files.class);) {
      mock.when(() -> Files.newOutputStream(any(), any())).thenThrow(IOException.class);
      Path path = Path.of("tmp/ddddasd/");
      InputStream in = IOUtils.toInputStream("test");
      assertThrows(FileSystemException.class, () -> FilesUtils.writeFile(in, "test", path));
    }
  }


  @Test
  void computeSha256_WhenNoSuchAlgorithmException_ThenThrowInternalException() {

    try (MockedStatic<MessageDigest> mock = Mockito.mockStatic(MessageDigest.class);) {
      mock.when(() -> MessageDigest.getInstance(any())).thenThrow(NoSuchAlgorithmException.class);
      Path path = Path.of("tmp/ddddasd/");
      assertThrows(InternalException.class, () -> FilesUtils.computeSha256(path));
    }
  }

}
