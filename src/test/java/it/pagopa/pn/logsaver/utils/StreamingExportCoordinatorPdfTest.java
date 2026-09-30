package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import com.lowagie.text.pdf.PdfReader;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator.UploadedPart;

class StreamingExportCoordinatorPdfTest {

  private static final Set<String> STANDARD_INFO_KEYS =
      Set.of("Title", "Subject", "Creator", "Author", "Producer", "CreationDate", "ModDate");

  private Path tmp;
  private List<Path> uploadedPaths;
  private StreamingExportCoordinator.PartUploader uploader;

  @BeforeEach
  void setUp() throws IOException {
    tmp = Files.createTempDirectory("wi4-coord-pdf-");
    uploadedPaths = new ArrayList<>();
    uploader = (part, retention, exportType) -> {
      Path kept = part.getParent().resolve("kept-" + part.getFileName());
      try {
        Files.copy(part, kept);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      uploadedPaths.add(kept);
      return "key-" + uploadedPaths.size();
    };
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(tmp.toFile());
  }

  @Test
  void append_whenSameEntryNameRecurs_onPdfBranch_keepsBothContents_insteadOfOverwriting()
      throws IOException {
    DailyContextCfg ctx = context(Map.of(Retention.DEVELOPER, Set.of(ExportType.PDF_SIGNED)));

    StreamingExportCoordinator coord =
        new StreamingExportCoordinator(ctx, DataSize.of(2, DataUnit.MEGABYTES), uploader);
    coord.accept(frag(Retention.DEVELOPER, "CONTENT FIRST", "same.log"));
    coord.accept(frag(Retention.DEVELOPER, "CONTENT OTHER", "other.log"));
    coord.accept(frag(Retention.DEVELOPER, "CONTENT SECOND", "same.log"));

    List<UploadedPart> res = coord.finish();
    assertEquals(1, res.size());

    Map<String, String> headers = readAllCustomHeaders(uploadedPaths);

    assertEquals(3, headers.size(),
        "sul ramo PDF la collisione sovrascriveva in silenzio: attese 3 voci, trovate " + headers);
    assertTrue(headers.get("same.log").contains("CONTENT FIRST"));
    assertTrue(headers.get("same.log~2").contains("CONTENT SECOND"),
        "il secondo contenuto omonimo deve essere conservato sotto nome derivato");
    assertTrue(headers.get("other.log").contains("CONTENT OTHER"));
  }

  private ClassifiedLogFragment frag(Retention retention, String content, String name) {
    return new ClassifiedLogFragment(retention, content.getBytes(StandardCharsets.UTF_8), name);
  }

  private DailyContextCfg context(Map<Retention, Set<ExportType>> map) {
    DailyContextCfg ctx = DailyContextCfg.builder().retentionExportTypeMap(map)
        .tmpBasePath(tmp.toString()).logFileTypes(Set.of(LogFileType.LOGS))
        .logDate(LocalDate.parse("2024-01-15")).build();
    ctx.initContext();
    return ctx;
  }

  private Map<String, String> readAllCustomHeaders(List<Path> parts) throws IOException {
    Map<String, String> result = new HashMap<>();
    for (Path part : parts) {
      try (PdfReader reader = new PdfReader(part.toString())) {
        reader.getInfo().entrySet().stream()
            .filter(entry -> !STANDARD_INFO_KEYS.contains(entry.getKey()))
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
      }
    }
    return result;
  }
}
