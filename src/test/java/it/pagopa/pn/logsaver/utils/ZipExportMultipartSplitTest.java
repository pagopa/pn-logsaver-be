package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import it.pagopa.pn.logsaver.exceptions.FileSystemException;

class ZipExportMultipartSplitTest {

  private Path folderIn;
  private Path folderOut;

  @BeforeEach
  void setUp() throws IOException {
    folderIn = Files.createTempDirectory("wi3-zip-in-");
    folderOut = Files.createTempDirectory("wi3-zip-out-");
    for (int i = 0; i < 5; i++) {
      Files.write(folderIn.resolve("f" + i + ".log"), new byte[200_000]);
    }
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(folderIn.toFile());
    FileUtils.deleteDirectory(folderOut.toFile());
  }

  @Test
  void export_shouldNotOverSplit_norCreateEmptyParts_whenContentIsCompressible() throws IOException {
    ZipExportMultipart export = new ZipExportMultipart(folderIn,
        DataSize.of(50, DataUnit.KILOBYTES), folderOut, "part%d.zip");

    List<Path> parts = export.export();

    for (Path part : parts) {
      assertTrue(countZipEntries(part) > 0, "parte zip senza entry: " + part);
    }
    assertEquals(1, parts.size(), "over-split: atteso 1 parte, prodotte " + parts.size());
  }

  @Test
  void append_whenContentReadFails_doesNotAddSpuriousEntry_norCorruptPart() throws IOException {
    ZipExportMultipart export = new ZipExportMultipart(folderIn,
        DataSize.of(50, DataUnit.MEGABYTES), folderOut, "part%d.zip");
    List<Path> closedParts = new ArrayList<>();
    export.setOnPartClosed(closedParts::add);

    export.append("a.log", new ByteArrayInputStream("AAA".getBytes(StandardCharsets.UTF_8)));
    assertThrows(FileSystemException.class,
        () -> export.append("bad.log", failingInputStream()));
    export.append("c.log", new ByteArrayInputStream("CCC".getBytes(StandardCharsets.UTF_8)));
    export.closeStream();

    assertEquals(1, closedParts.size(), "attesa 1 parte chiusa");
    assertEquals(List.of("a.log", "c.log"), zipEntryNames(closedParts.get(0)),
        "entry spuria/troncata prodotta da un fragment fallito in lettura");
  }

  private static InputStream failingInputStream() {
    return new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("read boom");
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        throw new IOException("read boom");
      }
    };
  }

  private List<String> zipEntryNames(Path part) throws IOException {
    List<String> names = new ArrayList<>();
    try (InputStream fis = Files.newInputStream(part);
        ZipInputStream zis = new ZipInputStream(fis)) {
      ZipEntry e;
      while ((e = zis.getNextEntry()) != null) {
        names.add(e.getName());
        zis.closeEntry();
      }
    }
    return names;
  }

  private int countZipEntries(Path part) throws IOException {
    int count = 0;
    try (InputStream fis = Files.newInputStream(part);
        ZipInputStream zis = new ZipInputStream(fis)) {
      while (zis.getNextEntry() != null) {
        count++;
        zis.closeEntry();
      }
    }
    return count;
  }
}
