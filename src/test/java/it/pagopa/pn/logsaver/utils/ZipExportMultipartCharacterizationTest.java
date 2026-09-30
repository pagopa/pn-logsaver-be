package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

class ZipExportMultipartCharacterizationTest {

  private Path folderIn;
  private Path folderOut;
  private Map<String, byte[]> sourceFiles;

  @BeforeEach
  void setUp() throws IOException {
    folderIn = Files.createTempDirectory("wi0-zip-in-");
    folderOut = Files.createTempDirectory("wi0-zip-out-");
    sourceFiles = new HashMap<>();
    sourceFiles.put("a.log", fixedBytes(10));
    sourceFiles.put("b.log", fixedBytes(50_000));
    sourceFiles.put("c.log", fixedBytes(30_000));
    sourceFiles.put("d.log", fixedBytes(20_000));
    for (Map.Entry<String, byte[]> entry : sourceFiles.entrySet()) {
      Files.write(folderIn.resolve(entry.getKey()), entry.getValue());
    }
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(folderIn.toFile());
    FileUtils.deleteDirectory(folderOut.toFile());
  }

  private byte[] fixedBytes(int size) {
    byte[] bytes = new byte[size];
    for (int i = 0; i < size; i++) {
      bytes[i] = (byte) (i % 251);
    }
    return bytes;
  }

  @Test
  @DisplayName("Contenuto complessivo (multiset entry->bytes) preservato su tutte le parti prodotte")
  void exportPreservesContentAcrossParts() throws IOException {
    ZipExportMultipart export = new ZipExportMultipart(folderIn,
        DataSize.of(40, DataUnit.KILOBYTES), folderOut, "test_part%d.zip");

    List<Path> parts = export.export();

    assertTrue(parts.size() >= 1);
    for (Path part : parts) {
      assertTrue(Files.exists(part));
    }

    Map<String, byte[]> mergedEntries = readAllZipEntries(parts);

    assertEquals(sourceFiles.size(), mergedEntries.size());
    for (Map.Entry<String, byte[]> expected : sourceFiles.entrySet()) {
      assertTrue(mergedEntries.containsKey(expected.getKey()));
      assertArrayEquals(expected.getValue(), mergedEntries.get(expected.getKey()));
    }
  }

  private Map<String, byte[]> readAllZipEntries(List<Path> parts) throws IOException {
    Map<String, byte[]> result = new HashMap<>();
    for (Path part : parts) {
      try (InputStream fis = Files.newInputStream(part);
          ZipInputStream zis = new ZipInputStream(fis)) {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
          result.put(entry.getName(), IOUtils.toByteArray(zis));
          zis.closeEntry();
        }
      }
    }
    return result;
  }
}
