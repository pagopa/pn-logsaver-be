package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.model.enums.Retention;

class PdfExportMultipartSplitTest {

  private Path folderIn;
  private Path folderOut;

  @BeforeEach
  void setUp() throws IOException {
    folderIn = Files.createTempDirectory("wi3-pdf-in-");
    folderOut = Files.createTempDirectory("wi3-pdf-out-");
    for (int i = 0; i < 10; i++) {
      Files.write(folderIn.resolve("audit-" + i + ".log"),
          ("AUDIT-CONTENT-" + i).getBytes(StandardCharsets.US_ASCII));
    }
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(folderIn.toFile());
    FileUtils.deleteDirectory(folderOut.toFile());
  }

  @Test
  void export_shouldNotOverSplit_whenRealPdfFitsMaxSize() throws IOException {
    PdfExportMultipart export = new PdfExportMultipart(folderIn, DataSize.ofBytes(8000), folderOut,
        "part%d.pdf", Retention.DEVELOPER, LocalDate.parse("2022-10-02"));

    List<Path> parts = export.export();

    for (Path part : parts) {
      assertTrue(Files.size(part) <= 8000, "parte oltre maxSize: " + Files.size(part));
    }
    assertEquals(1, parts.size(), "over-split: atteso 1 parte, prodotte " + parts.size());
  }
}
