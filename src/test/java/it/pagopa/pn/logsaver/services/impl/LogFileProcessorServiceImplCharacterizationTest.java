package it.pagopa.pn.logsaver.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import com.lowagie.text.pdf.PdfReader;
import it.pagopa.pn.logsaver.config.LogSaverCfg;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.LogFileType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.LogFileProcessorService;
import it.pagopa.pn.logsaver.services.LogFileReaderService;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator;

@ExtendWith(MockitoExtension.class)
class LogFileProcessorServiceImplCharacterizationTest {

  @Mock
  private LogFileReaderService s3Service;

  @Mock
  private LogSaverCfg cfg;

  private LogFileProcessorService service;
  private Path tmpBasePath;
  private Path keepDir;

  private static final LocalDate LOGDATE = LocalDate.parse("2022-07-11");

  @BeforeEach
  void setUp() throws IOException {
    service = new LogFileProcessorServiceImpl(s3Service);
    tmpBasePath = Files.createTempDirectory("wi0-logprocessor-");
    keepDir = Files.createTempDirectory("wi0-logprocessor-keep-");
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(tmpBasePath.toFile());
    FileUtils.deleteDirectory(keepDir.toFile());
  }

  @Test
  @DisplayName(
      "FROZEN: contenuto scritto (entry ZIP + header PDF) pinnato, prodotto via streaming coordinator")
  void processPinsWrittenContentThroughStreaming() throws IOException {
    LogFileReference item1 =
        LogFileReference.builder().logDate(LOGDATE).type(LogFileType.LOGS).s3Key("key-1").build();
    LogFileReference item2 =
        LogFileReference.builder().logDate(LOGDATE).type(LogFileType.LOGS).s3Key("key-2").build();

    when(s3Service.getContent("key-1"))
        .thenReturn(IOUtils.toInputStream("BUCKETFILE-1", StandardCharsets.US_ASCII));
    when(s3Service.getContent("key-2"))
        .thenReturn(IOUtils.toInputStream("BUCKETFILE-2", StandardCharsets.US_ASCII));

    BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> fixedFilter =
        (in, dailyCtx) -> {
          if ("key-1".equals(in.getS3Key())) {
            return Stream.of(
                new ClassifiedLogFragment(Retention.AUDIT10Y,
                    IOUtils.toInputStream("CONTENT-A-10Y", StandardCharsets.US_ASCII), "part1.log"),
                new ClassifiedLogFragment(Retention.DEVELOPER,
                    IOUtils.toInputStream("CONTENT-A-DEV", StandardCharsets.US_ASCII),
                    "part1-dev.log"));
          } else {
            return Stream.of(
                new ClassifiedLogFragment(Retention.AUDIT10Y,
                    IOUtils.toInputStream("CONTENT-B-10Y", StandardCharsets.US_ASCII), "part2.log"),
                new ClassifiedLogFragment(Retention.DEVELOPER,
                    IOUtils.toInputStream("CONTENT-B-DEV", StandardCharsets.US_ASCII),
                    "part2-dev.log"));
          }
        };
    ReflectionTestUtils.setField(LogFileType.LOGS, "filter", fixedFilter);

    Map<Retention, Set<ExportType>> retentionExportTypeMap = new LinkedHashMap<>();
    retentionExportTypeMap.put(Retention.AUDIT10Y, Set.of(ExportType.ZIP));
    retentionExportTypeMap.put(Retention.DEVELOPER, Set.of(ExportType.PDF_SIGNED));

    DailyContextCfg ctx = DailyContextCfg.builder().retentionExportTypeMap(retentionExportTypeMap)
        .tmpBasePath(tmpBasePath.toString()).logFileTypes(Set.of(LogFileType.LOGS)).logDate(LOGDATE)
        .build();
    ctx.initContext();

    List<Path> keptZip = new ArrayList<>();
    List<Path> keptPdf = new ArrayList<>();
    StreamingExportCoordinator.PartUploader uploader = (part, retention, exportType) -> {
      try {
        Path kept = keepDir.resolve(part.getFileName().toString());
        Files.copy(part, kept);
        if (ExportType.ZIP == exportType) {
          keptZip.add(kept);
        } else {
          keptPdf.add(kept);
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      return "key-" + (keptZip.size() + keptPdf.size());
    };

    StreamingExportCoordinator coordinator =
        new StreamingExportCoordinator(ctx, DataSize.of(10, DataUnit.MEGABYTES), uploader);

    service.process(Stream.of(item1, item2), ctx, coordinator);

    Map<String, byte[]> zipEntries = readAllZipEntries(keptZip);
    assertEquals(2, zipEntries.size());
    assertEquals("CONTENT-A-10Y", new String(zipEntries.get("part1.log"), StandardCharsets.US_ASCII));
    assertEquals("CONTENT-B-10Y", new String(zipEntries.get("part2.log"), StandardCharsets.US_ASCII));

    Map<String, String> pdfHeaders = readAllCustomHeaders(keptPdf);
    assertEquals(2, pdfHeaders.size());
    String expectedA = String.format("<audit date=\"%s\" fileName=\"%s\" retention=\"%s\"><![CDATA[",
        LOGDATE.toString(), "part1-dev.log", Retention.DEVELOPER.name()) + "CONTENT-A-DEV"
        + "]]></audit>";
    String expectedB = String.format("<audit date=\"%s\" fileName=\"%s\" retention=\"%s\"><![CDATA[",
        LOGDATE.toString(), "part2-dev.log", Retention.DEVELOPER.name()) + "CONTENT-B-DEV"
        + "]]></audit>";
    assertEquals(expectedA, pdfHeaders.get("part1-dev.log"));
    assertEquals(expectedB, pdfHeaders.get("part2-dev.log"));
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

  private static final Set<String> STANDARD_INFO_KEYS =
      Set.of("Title", "Subject", "Creator", "Author", "Producer", "CreationDate", "ModDate");

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
