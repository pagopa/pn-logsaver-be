package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Random;
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
    InputStream failingStream = failingInputStream();
    assertThrows(FileSystemException.class, () -> export.append("bad.log", failingStream));
    export.append("c.log", new ByteArrayInputStream("CCC".getBytes(StandardCharsets.UTF_8)));
    export.closeStream();

    assertEquals(1, closedParts.size(), "attesa 1 parte chiusa");
    assertEquals(List.of("a.log", "c.log"), zipEntryNames(closedParts.get(0)),
        "entry spuria/troncata prodotta da un fragment fallito in lettura");
  }

  @Test
  void append_shouldNotExceedMaxSize_whenEachEntryFitsIndividually() throws IOException {
    DataSize limit = DataSize.of(50, DataUnit.KILOBYTES);
    ZipExportMultipart export =
        new ZipExportMultipart(folderIn, limit, folderOut, "part%d.zip");
    List<Path> closedParts = new ArrayList<>();
    export.setOnPartClosed(closedParts::add);

    for (int i = 0; i < 10; i++) {
      export.append("e" + i + ".log", new ByteArrayInputStream(incompressible(20_000)));
    }
    export.closeStream();

    for (Path part : closedParts) {
      long size = Files.size(part);
      assertTrue(size <= limit.toBytes(), "parte oltre il limite configurato: "
          + part.getFileName() + " occupa " + size + " byte contro " + limit.toBytes());
    }
  }

  @Test
  void append_shouldKeepOversizedEntry_inItsOwnPart() throws IOException {
    DataSize limit = DataSize.of(20, DataUnit.KILOBYTES);
    ZipExportMultipart export =
        new ZipExportMultipart(folderIn, limit, folderOut, "part%d.zip");
    List<Path> closedParts = new ArrayList<>();
    export.setOnPartClosed(closedParts::add);

    export.append("small.log", new ByteArrayInputStream(incompressible(1_000)));
    export.append("oversized.log", new ByteArrayInputStream(incompressible(100_000)));
    export.append("coda.log", new ByteArrayInputStream(incompressible(1_000)));
    export.closeStream();

    List<String> allEntryNames = new ArrayList<>();
    for (Path part : closedParts) {
      assertTrue(countZipEntries(part) > 0, "parte zip senza entry: " + part);
      allEntryNames.addAll(zipEntryNames(part));
    }
    assertTrue(allEntryNames.contains("oversized.log"),
        "l'entry piu' grande del limite non deve essere persa: presenti " + allEntryNames);
    assertEquals(3, allEntryNames.size(), "tutte le entry devono essere scritte: " + allEntryNames);
  }

  @Test
  void append_whenWriteFails_doesNotPublishCorruptPart() throws IOException {
    List<Path> closedParts = new ArrayList<>();
    List<Path> discardedParts = new ArrayList<>();
    ZipExportMultipart export = exportFailingOn("ko", closedParts, discardedParts);

    export.append("ok.log", new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));
    InputStream brokenContent = new ByteArrayInputStream("KO".getBytes(StandardCharsets.UTF_8));
    assertThrows(FileSystemException.class, () -> export.append("ko.log", brokenContent));
    export.append("third.log", new ByteArrayInputStream("THIRD".getBytes(StandardCharsets.UTF_8)));
    export.closeStream();

    assertEquals(1, discardedParts.size(),
        "la parte compromessa dalla scrittura interrotta deve essere scartata");
    assertFalse(Files.exists(discardedParts.get(0)),
        "la parte scartata deve essere rimossa dal disco");
    for (Path part : closedParts) {
      assertFalse(zipEntryNames(part).contains("ko.log"),
          "una parte pubblicata contiene la entry scritta a meta': " + part);
    }
    assertEquals(List.of("third.log"), zipEntryNames(closedParts.get(closedParts.size() - 1)),
        "dopo lo scarto la lavorazione deve proseguire su una parte nuova");
  }

  @Test
  void append_whenWriteFails_releasesTheReservedEntryName() throws IOException {
    List<Path> closedParts = new ArrayList<>();
    ZipExportMultipart export = exportFailingOn("first-attempt", closedParts,
        new ArrayList<>());

    InputStream firstContent = new ByteArrayInputStream("X".getBytes(StandardCharsets.UTF_8));
    assertThrows(FileSystemException.class,
        () -> export.append("first-attempt.log", firstContent));
    export.append("first-attempt.log",
        new ByteArrayInputStream("Y".getBytes(StandardCharsets.UTF_8)));
    export.closeStream();

    assertEquals(List.of("first-attempt.log"), zipEntryNames(closedParts.get(0)),
        "il nome riservato da un tentativo fallito non e' stato liberato");
  }

  @Test
  void append_whenPartIsClosed_theDeclaredSizeMatchesTheFileOnDisk() throws IOException {
    List<Path> closedParts = new ArrayList<>();
    ZipExportMultipart export = new ZipExportMultipart(folderIn,
        DataSize.of(50, DataUnit.MEGABYTES), folderOut, "part%d.zip");
    export.setOnPartClosed(closedParts::add);

    for (int i = 0; i < 100; i++) {
      export.append("logs/ecs/pnDelivery/2022/07/11/12/log-" + i + ".log",
          new ByteArrayInputStream(("content " + i).getBytes(StandardCharsets.UTF_8)));
    }
    long declaredSize = export.currentPartSize();
    export.closeStream();

    assertEquals(declaredSize, Files.size(closedParts.get(0)),
        "la dimensione dichiarata durante la scrittura non tiene conto dell'indice finale");
  }

  @Test
  void append_manySmallEntries_keepsEveryPartWithinTheLimit() throws IOException {
    DataSize limit = DataSize.of(200, DataUnit.KILOBYTES);
    List<Path> closedParts = new ArrayList<>();
    ZipExportMultipart export =
        new ZipExportMultipart(folderIn, limit, folderOut, "part%d.zip");
    export.setOnPartClosed(closedParts::add);

    for (int i = 0; i < 3000; i++) {
      export.append("logs/ecs/pnDelivery/2022/07/11/12/log-" + i + ".log",
          new ByteArrayInputStream(("line " + i).getBytes(StandardCharsets.UTF_8)));
    }
    export.closeStream();

    for (Path part : closedParts) {
      assertTrue(Files.size(part) <= limit.toBytes(),
          "parte oltre il limite: " + Files.size(part) + " byte contro " + limit.toBytes());
    }
  }

  private ZipExportMultipart exportFailingOn(String prefixToFail, List<Path> closedParts,
      List<Path> discardedParts) {
    ZipExportMultipart export = new ZipExportMultipart(folderIn,
        DataSize.of(50, DataUnit.MEGABYTES), folderOut, "part%d.zip") {
      private boolean alreadyFailed;

      @Override
      protected void addLogEntry(String entryName, InputStream content) throws IOException {
        if (entryName.startsWith(prefixToFail) && !alreadyFailed) {
          alreadyFailed = true;
          currentFileOut.putNextEntry(new ZipEntry(entryName));
          currentFileOut.write("PARTIAL".getBytes(StandardCharsets.UTF_8));
          throw new IOException("write interrupted halfway");
        }
        super.addLogEntry(entryName, content);
      }
    };
    export.setOnPartClosed(closedParts::add);
    export.setOnPartDiscarded((part, cause) -> discardedParts.add(part));
    return export;
  }

  private static byte[] incompressible(int n) {
    byte[] data = new byte[n];
    new Random(42L).nextBytes(data);
    return data;
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
