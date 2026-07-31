package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import com.lowagie.text.pdf.PdfReader;
import it.pagopa.pn.logsaver.model.enums.Retention;

class PdfExportMultipartCharacterizationTest {

  private static final Set<String> STANDARD_INFO_KEYS =
      Set.of("Title", "Subject", "Creator", "Author", "Producer", "CreationDate", "ModDate");

  private static final String START_XML_AUDIT =
      "<audit date=\"%s\" fileName=\"%s\" retention=\"%s\"><![CDATA[";
  private static final String END_XML_AUDIT = "]]></audit>";

  private Path folderIn;
  private Path folderOut;
  private Map<String, String> sourceFiles;
  private Retention retention;
  private LocalDate logDate;

  @BeforeEach
  void setUp() throws IOException {
    folderIn = Files.createTempDirectory("wi0-pdf-in-");
    folderOut = Files.createTempDirectory("wi0-pdf-out-");
    retention = Retention.DEVELOPER;
    logDate = LocalDate.parse("2022-10-02");

    sourceFiles = new HashMap<>();
    sourceFiles.put("audit-a.log", "CONTENT AUDIT A");
    sourceFiles.put("audit-b.log", "CONTENT AUDIT B");
    sourceFiles.put("audit-c.log", "CONTENT AUDIT C");
    for (Map.Entry<String, String> entry : sourceFiles.entrySet()) {
      Files.write(folderIn.resolve(entry.getKey()),
          entry.getValue().getBytes(StandardCharsets.US_ASCII));
    }
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(folderIn.toFile());
    FileUtils.deleteDirectory(folderOut.toFile());
  }

  @Test
  @DisplayName("Contenuto testuale (Header XML) preservato su tutte le parti prodotte")
  void exportPreservesTextualContentAcrossParts() throws IOException {
    PdfExportMultipart export = new PdfExportMultipart(folderIn, DataSize.ofBytes(3000), folderOut,
        "test_part%d.pdf", retention, logDate);

    List<Path> parts = export.export();

    assertTrue(parts.size() >= 1);
    for (Path part : parts) {
      assertTrue(Files.exists(part));
    }

    Map<String, String> mergedHeaders = readAllCustomHeaders(parts);

    assertEquals(sourceFiles.size(), mergedHeaders.size());
    for (Map.Entry<String, String> expected : sourceFiles.entrySet()) {
      String expectedXml = String.format(START_XML_AUDIT, logDate.toString(), expected.getKey(),
          retention.name()) + expected.getValue() + END_XML_AUDIT;
      assertTrue(mergedHeaders.containsKey(expected.getKey()));
      assertEquals(expectedXml, mergedHeaders.get(expected.getKey()));
    }
  }

  private Map<String, String> readAllCustomHeaders(List<Path> parts) throws IOException {
    Map<String, String> result = new HashMap<>();
    for (Path part : parts) {
      PdfReader reader = new PdfReader(part.toString());
      try {
        Map<String, String> info = reader.getInfo();
        info.entrySet().stream().filter(entry -> !STANDARD_INFO_KEYS.contains(entry.getKey()))
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
      } finally {
        reader.close();
      }
    }
    return result;
  }
}
