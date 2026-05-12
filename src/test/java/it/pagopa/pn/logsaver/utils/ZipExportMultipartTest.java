package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class ZipExportMultipartTest {

  private Path folderIn;
  private Path folderOut;

  @BeforeEach
  void setUp() throws IOException {
    folderIn = Files.createTempDirectory("zip-export-in-");
    folderOut = Files.createTempDirectory("zip-export-out-");
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.forceDelete(folderIn.toFile());
    FileUtils.forceDelete(folderOut.toFile());
  }

  @Test
  void export_WhenAllFilesFitInOne_ThenProducesOneZip() throws IOException {
    writeFile(folderIn, "a.json", "x".repeat(100));
    writeFile(folderIn, "b.json", "y".repeat(100));

    ZipExportMultipart exporter = new ZipExportMultipart(
        folderIn, DataSize.ofKilobytes(100), folderOut, "archive_part%d.zip");

    List<Path> result = exporter.export();

    assertEquals(1, result.size());
    assertTrue(Files.exists(result.get(0)));
    assertZipEntryCount(result.get(0), 2);
  }

  @Test
  void export_WhenFilesExceedMaxSize_ThenSplitsIntoMultipleParts() throws IOException {
    writeRandomFile(folderIn, "a.json", 10_000);
    writeRandomFile(folderIn, "b.json", 10_000);
    writeRandomFile(folderIn, "c.json", 10_000);

    ZipExportMultipart exporter = new ZipExportMultipart(
        folderIn, DataSize.ofBytes(5000), folderOut, "archive_part%d.zip");

    List<Path> result = exporter.export();

    assertTrue(result.size() > 1, "Attesi piu' part, ottenuti: " + result.size());
    for (Path p : result) {
      assertTrue(Files.exists(p));
      assertTrue(Files.size(p) > 0, "Part vuoto: " + p);
    }
  }

  @Test
  void export_WhenSingleFileLargerThanMaxSize_ThenNoEmptyPart() throws IOException {
    writeFile(folderIn, "big.json", "z".repeat(10_000));

    ZipExportMultipart exporter = new ZipExportMultipart(
        folderIn, DataSize.ofBytes(100), folderOut, "archive_part%d.zip");

    List<Path> result = exporter.export();

    assertEquals(1, result.size(), "Un file grande deve stare in un unico part (non generare part vuoti)");
    assertTrue(Files.size(result.get(0)) > 0, "Il part non deve essere vuoto");
    assertZipEntryCount(result.get(0), 1);
  }

  @Test
  void export_WhenMultipleFilesEachLargerThanMaxSize_ThenOneFilePerPart() throws IOException {
    writeFile(folderIn, "a.json", "abc".repeat(5_000));
    writeFile(folderIn, "b.json", "abc".repeat(5_000));

    ZipExportMultipart exporter = new ZipExportMultipart(
        folderIn, DataSize.ofBytes(100), folderOut, "archive_part%d.zip");

    List<Path> result = exporter.export();

    assertEquals(2, result.size());
    for (Path p : result) {
      assertTrue(Files.size(p) > 0, "Part vuoto: " + p);
      assertZipEntryCount(p, 1);
    }
  }

  private void writeFile(Path dir, String name, String content) throws IOException {
    Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
  }

  private void writeRandomFile(Path dir, String name, int sizeBytes) throws IOException {
    byte[] data = new byte[sizeBytes];
    new Random(42).nextBytes(data);
    Files.write(dir.resolve(name), data);
  }

  private void assertZipEntryCount(Path zipPath, int expected) throws IOException {
    try (ZipFile zf = new ZipFile(zipPath.toFile())) {
      assertEquals(expected, zf.size(), "Numero entry nel ZIP: " + zipPath.getFileName());
    }
  }
}
