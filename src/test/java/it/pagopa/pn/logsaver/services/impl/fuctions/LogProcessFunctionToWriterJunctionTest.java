package it.pagopa.pn.logsaver.services.impl.fuctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import com.google.gson.JsonStreamParser;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.impl.functions.LogProcessFunction;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator.UploadedPart;

@ExtendWith(SpringExtension.class)
class LogProcessFunctionToWriterJunctionTest {

  @Value(TestCostant.FILE_LOG)
  private Resource multiRecordFile;

  private Path tmp;
  private Map<String, byte[]> entriesByName;
  private List<Path> uploadedPaths;
  private StreamingExportCoordinator.PartUploader uploader;

  @BeforeEach
  void setUp() throws IOException {
    tmp = Files.createTempDirectory("junction-");
    entriesByName = new LinkedHashMap<>();
    uploadedPaths = new ArrayList<>();
    uploader = (part, retention, exportType) -> {
      uploadedPaths.add(part);
      entriesByName.putAll(readZipEntries(part));
      return "key-" + uploadedPaths.size();
    };
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(tmp.toFile());
  }

  @Test
  void multiRecordFile_producesOneEntryPerRetention_withEveryRecordPreserved() throws IOException {
    DailyContextCfg ctx = DailyContextCfg.builder()
        .retentionExportTypeMap(Map.of(Retention.AUDIT10Y, Set.of(ExportType.ZIP)))
        .tmpBasePath(tmp.toString()).logFileTypes(Set.of(LogFileType.LOGS))
        .logDate(TestCostant.LOGDATE).build();
    ctx.initContext();

    LogFileReference item = LogFileReference.builder().logDate(TestCostant.LOGDATE)
        .type(LogFileType.LOGS).s3Key(TestCostant.S3_KEY).build();

    StreamingExportCoordinator coord =
        new StreamingExportCoordinator(ctx, DataSize.of(2, DataUnit.MEGABYTES), uploader);
    try (Stream<ClassifiedLogFragment> fragments = new LogProcessFunction().apply(item, multiRecordFile.getInputStream(), ctx)) {
      fragments.forEach(coord::accept);
    }
    List<UploadedPart> parts = coord.finish();

    assertEquals(1, parts.size());
    String expectedEntry = FilenameUtils.getBaseName(TestCostant.S3_KEY);
    assertEquals(List.of(expectedEntry), List.copyOf(entriesByName.keySet()),
        "un file multi-record deve produrre una sola entry, non una per record");

    assertEquals(List.of(4, 4), logEventSizes(entriesByName.get(expectedEntry)),
        "tutti i record top-level del file devono finire nell'archivio, nell'ordine di lettura");
  }

  private List<Integer> logEventSizes(byte[] entryContent) {
    assertTrue(entryContent != null && entryContent.length > 0, "entry vuota");
    List<Integer> sizes = new ArrayList<>();
    JsonStreamParser parser = new JsonStreamParser(new String(entryContent, StandardCharsets.UTF_8));
    while (parser.hasNext()) {
      sizes.add(parser.next().getAsJsonObject().getAsJsonArray("logEvents").size());
    }
    return sizes;
  }

  private Map<String, byte[]> readZipEntries(Path part) {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    try (InputStream fis = Files.newInputStream(part); ZipInputStream zis = new ZipInputStream(fis)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(zis, out);
        entries.put(entry.getName(), out.toByteArray());
        zis.closeEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return entries;
  }
}
