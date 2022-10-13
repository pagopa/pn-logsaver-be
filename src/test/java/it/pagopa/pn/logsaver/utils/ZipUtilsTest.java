package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;

@ExtendWith(SoftAssertionsExtension.class)
class ZipUtilsTest {



  @Test
  void createZip_WhenIOException_ThenThrowFileSystemException() {

    try (MockedStatic<Files> mock = Mockito.mockStatic(Files.class);) {
      mock.when(() -> Files.newOutputStream(any(), any())).thenThrow(IOException.class);
      Path pathI = Path.of("tmp/test");
      Path pathO = Path.of("tmp/test/test.zip");
      assertThrows(FileSystemException.class, () -> ZipUtils.createZip(pathI, pathO));

    }
  }

  @Test
  void createZip() throws IOException {
    Path pathI = Path.of("tmp/test");
    Path pathO = Path.of("tmp/test/test.zip");
    Files.createDirectories(Path.of("tmp/test/test"));
    ZipUtils.createZip(pathI, pathO);

    assertTrue(Files.exists(pathO));
    FileUtils.forceDelete(pathI.toFile());
  }
}
