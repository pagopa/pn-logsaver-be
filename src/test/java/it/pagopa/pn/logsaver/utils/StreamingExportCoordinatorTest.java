package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator.UploadedPart;

class StreamingExportCoordinatorTest {

  private Path tmp;
  private List<Path> uploadedPaths;
  private Map<String, List<String>> entriesByPart;
  private StreamingExportCoordinator.PartUploader uploader;

  @BeforeEach
  void setUp() throws IOException {
    tmp = Files.createTempDirectory("wi4-coord-");
    uploadedPaths = new ArrayList<>();
    entriesByPart = new LinkedHashMap<>();
    uploader = (part, retention, exportType) -> {
      uploadedPaths.add(part);
      entriesByPart.put(part.getFileName().toString(), readZipEntryNames(part));
      return "key-" + uploadedPaths.size();
    };
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(tmp.toFile());
  }

  private ClassifiedLogFragment frag(Retention retention, String content, String name) {
    InputStream is = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
    return new ClassifiedLogFragment(retention, is, name);
  }

  private DailyContextCfg context(Map<Retention, Set<ExportType>> map) {
    DailyContextCfg ctx = DailyContextCfg.builder().retentionExportTypeMap(map)
        .tmpBasePath(tmp.toString()).logFileTypes(Set.of(LogFileType.LOGS))
        .logDate(LocalDate.parse("2024-01-15")).build();
    ctx.initContext();
    return ctx;
  }

  private List<String> readZipEntryNames(Path part) {
    List<String> names = new ArrayList<>();
    try (InputStream fis = Files.newInputStream(part); ZipInputStream zis = new ZipInputStream(fis)) {
      ZipEntry e;
      while ((e = zis.getNextEntry()) != null) {
        names.add(e.getName());
        zis.closeEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return names;
  }

  @Test
  void append_whenSameEntryNameRecursNonContiguously_keepsBothContents_underDerivedName()
      throws IOException {
    DailyContextCfg ctx = context(Map.of(Retention.AUDIT10Y, Set.of(ExportType.ZIP)));

    StreamingExportCoordinator coord =
        new StreamingExportCoordinator(ctx, DataSize.of(2, DataUnit.MEGABYTES), uploader);
    coord.accept(frag(Retention.AUDIT10Y, "FIRST", "same.log"));
    coord.accept(frag(Retention.AUDIT10Y, "OTHER", "other.log"));
    coord.accept(frag(Retention.AUDIT10Y, "SECOND", "same.log"));

    List<UploadedPart> res = coord.finish();

    assertEquals(1, res.size());
    assertEquals(List.of("same.log", "other.log", "same.log~2"),
        entriesByPart.get(res.get(0).partName()),
        "nessuna entry deve essere scartata (ZIP) ne sovrascritta (PDF)");
  }

  @Test
  void accept_routesPerRetention_uploadsAndDeletesParts_withoutSourceStaging() throws IOException {
    DailyContextCfg ctx = context(Map.of(Retention.AUDIT10Y, Set.of(ExportType.ZIP),
        Retention.DEVELOPER, Set.of(ExportType.ZIP)));

    StreamingExportCoordinator coord =
        new StreamingExportCoordinator(ctx, DataSize.of(2, DataUnit.MEGABYTES), uploader);
    coord.accept(frag(Retention.AUDIT10Y, "CONTENT-A", "a.log"));
    coord.accept(frag(Retention.DEVELOPER, "CONTENT-D", "d.log"));
    coord.accept(frag(Retention.AUDIT10Y, "CONTENT-A2", "a2.log"));

    List<UploadedPart> res = coord.finish();

    assertEquals(2, res.size(), "atteso 1 parte finale per writer (2 retention)");

    for (Path p : uploadedPaths) {
      assertFalse(Files.exists(p), "parte non cancellata dopo upload: " + p);
    }

    for (Path retDir : ctx.retentionTmpFolder().values()) {
      try (Stream<Path> s = Files.list(retDir)) {
        assertEquals(0, s.count(), "staging sorgenti su disco in " + retDir);
      }
    }

    Map<Retention, Set<String>> expected = Map.of(Retention.AUDIT10Y, Set.of("a.log", "a2.log"),
        Retention.DEVELOPER, Set.of("d.log"));
    for (UploadedPart up : res) {
      List<String> entries = entriesByPart.get(up.partName());
      assertNotNull(entries, "parte non caricata: " + up.partName());
      assertTrue(expected.get(up.retention()).containsAll(entries),
          "cross-retention nella parte " + up.partName() + ": " + entries);
    }
  }

  @Test
  void finish_whenUploadFails_isolatesFailurePerWriter_closesOthers_reportsFailedPart() {
    DailyContextCfg ctx = context(Map.of(Retention.AUDIT10Y, Set.of(ExportType.ZIP),
        Retention.DEVELOPER, Set.of(ExportType.ZIP)));

    StreamingExportCoordinator.PartUploader failing = (part, retention, exportType) -> {
      if (retention == Retention.DEVELOPER) {
        throw new RuntimeException("SafeStorage 500");
      }
      uploadedPaths.add(part);
      return "key-" + uploadedPaths.size();
    };

    StreamingExportCoordinator coord =
        new StreamingExportCoordinator(ctx, DataSize.of(2, DataUnit.MEGABYTES), failing);
    coord.accept(frag(Retention.AUDIT10Y, "CONTENT-A", "a.log"));
    coord.accept(frag(Retention.DEVELOPER, "CONTENT-D", "d.log"));

    List<UploadedPart> res = assertDoesNotThrow(coord::finish);

    UploadedPart ok = res.stream().filter(p -> p.retention() == Retention.AUDIT10Y).findFirst()
        .orElseThrow();
    assertNull(ok.error(), "la parte riuscita non deve avere errore");

    UploadedPart failed = res.stream().filter(p -> p.retention() == Retention.DEVELOPER).findFirst()
        .orElseThrow();
    assertNotNull(failed.error(), "la parte fallita deve riportare l'errore (per marcare CREATED)");
  }

  @Test
  void finish_tracksPeakTmpDiskUsage_atLeastLargestPartOnDisk() {
    DailyContextCfg ctx = context(Map.of(Retention.AUDIT10Y, Set.of(ExportType.ZIP)));

    List<Long> partSizesOnDisk = new ArrayList<>();
    StreamingExportCoordinator.PartUploader sizingUploader = (part, retention, exportType) -> {
      try {
        partSizesOnDisk.add(Files.size(part));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      return "key-" + partSizesOnDisk.size();
    };

    StreamingExportCoordinator coord =
        new StreamingExportCoordinator(ctx, DataSize.ofBytes(500), sizingUploader);
    for (int i = 0; i < 20; i++) {
      coord.accept(
          frag(Retention.AUDIT10Y, RandomStringUtils.randomAlphanumeric(2000), "f" + i + ".log"));
    }
    coord.finish();

    long maxPartOnDisk = partSizesOnDisk.stream().mapToLong(Long::longValue).max().orElse(0L);
    assertTrue(maxPartOnDisk > 0, "precondizione: almeno una parte scritta su disco");
    assertTrue(coord.getPeakTmpBytes() >= maxPartOnDisk,
        "picco disco tmp deve essere >= alla parte piu' grande su disco: peak="
            + coord.getPeakTmpBytes() + " maxPart=" + maxPartOnDisk);
  }

  @Test
  void accept_uploadsPartsInFlight_onRotation_beforeFinish() {
    DailyContextCfg ctx = context(Map.of(Retention.AUDIT10Y, Set.of(ExportType.ZIP)));

    StreamingExportCoordinator coord =
        new StreamingExportCoordinator(ctx, DataSize.ofBytes(500), uploader);
    for (int i = 0; i < 20; i++) {
      coord.accept(frag(Retention.AUDIT10Y, RandomStringUtils.randomAlphanumeric(2000), "f" + i + ".log"));
    }

    int uploadsBeforeFinish = uploadedPaths.size();
    coord.finish();

    assertTrue(uploadsBeforeFinish >= 1,
        "nessun upload in volo prima di finish (rotazione non attiva)");
  }
}
