package it.pagopa.pn.logsaver.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.springframework.util.unit.DataSize;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamingExportCoordinator {

  public record UploadedPart(Retention retention, ExportType exportType, String storageKey,
      String partName, Throwable error) {
  }

  @FunctionalInterface
  public interface PartUploader {
    String upload(Path part, Retention retention, ExportType exportType);
  }

  private record WriterKey(Retention retention, ExportType exportType) {
  }

  private static final String EMPTY_RETENTION_ENTRY_NAME = "Readme.md";
  private static final byte[] EMPTY_RETENTION_ENTRY_CONTENT =
      ("Log file not found" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);

  private final DailyContextCfg dailyCtx;
  private final DataSize maxSize;
  private final PartUploader uploader;
  private final Map<WriterKey, AbstractExportMultipart<?>> writers = new LinkedHashMap<>();
  private final List<UploadedPart> uploaded = new ArrayList<>();
  private long peakTmpBytes = 0;

  public StreamingExportCoordinator(DailyContextCfg dailyCtx, DataSize maxSize,
      PartUploader uploader) {
    this.dailyCtx = dailyCtx;
    this.maxSize = maxSize;
    this.uploader = uploader;
  }

  public void accept(ClassifiedLogFragment fragment) {
    Retention retention = fragment.getRetention();
    Set<ExportType> exportTypes = dailyCtx.getExportTypesByRetention(retention);
    if (exportTypes == null || exportTypes.isEmpty()) {
      return;
    }
    byte[] bytes;
    try {
      bytes = IOUtils.toByteArray(fragment.getContent());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    for (ExportType exportType : exportTypes) {
      AbstractExportMultipart<?> writer =
          writers.computeIfAbsent(new WriterKey(retention, exportType),
              key -> createWriter(retention, exportType));
      writer.append(fragment.getFileName(), new ByteArrayInputStream(bytes));
    }
  }

  public List<UploadedPart> finish() {
    for (Retention retention : dailyCtx.retentions()) {
      Set<ExportType> exportTypes = dailyCtx.getExportTypesByRetention(retention);
      if (exportTypes == null) {
        continue;
      }
      for (ExportType exportType : exportTypes) {
        WriterKey key = new WriterKey(retention, exportType);
        if (!writers.containsKey(key)) {
          AbstractExportMultipart<?> writer = createWriter(retention, exportType);
          writers.put(key, writer);
          writer.append(EMPTY_RETENTION_ENTRY_NAME,
              new ByteArrayInputStream(EMPTY_RETENTION_ENTRY_CONTENT));
        }
      }
    }
    for (AbstractExportMultipart<?> writer : writers.values()) {
      writer.closeStream();
    }
    log.info("Peak tmp disk usage: {} bytes for date {}", peakTmpBytes, dailyCtx.logDate());
    return uploaded;
  }

  public long getPeakTmpBytes() {
    return peakTmpBytes;
  }

  private long currentTmpBytes() {
    Path dir = dailyCtx.tmpDailyPath();
    if (!Files.exists(dir)) {
      return 0L;
    }
    try (Stream<Path> walk = Files.walk(dir)) {
      return walk.filter(Files::isRegularFile).mapToLong(p -> {
        try {
          return Files.size(p);
        } catch (IOException e) {
          return 0L;
        }
      }).sum();
    } catch (IOException e) {
      log.warn("Cannot measure tmp disk usage in {}: {}", dir, e.getMessage());
      return 0L;
    }
  }

  private AbstractExportMultipart<?> createWriter(Retention retention, ExportType exportType) {
    Path folderOut = dailyCtx.tmpDailyPath();
    String patternFileOut = dailyCtx.logDate()
        .format(DateTimeFormatter.ofPattern(retention.getFileNamePattern()))
        .concat(exportType.getExtension());

    AbstractExportMultipart<?> writer;
    if (ExportType.PDF_SIGNED == exportType) {
      writer = new PdfExportMultipart(folderOut, maxSize, folderOut, patternFileOut, retention,
          dailyCtx.logDate());
    } else {
      writer = new ZipExportMultipart(folderOut, maxSize, folderOut, patternFileOut);
    }

    writer.setOnPartClosed(part -> {
      peakTmpBytes = Math.max(peakTmpBytes, currentTmpBytes());
      String storageKey = null;
      Throwable error = null;
      try {
        storageKey = uploader.upload(part, retention, exportType);
      } catch (Exception e) {
        error = e;
        log.warn("Upload part {} failed for retention {} exportType {}: {}",
            part.getFileName(), retention, exportType, e.getMessage());
      }
      try {
        Files.deleteIfExists(part);
      } catch (IOException e) {
        log.warn("Cannot delete part {}: {}", part, e.getMessage());
      }
      uploaded.add(
          new UploadedPart(retention, exportType, storageKey, part.getFileName().toString(), error));
    });

    return writer;
  }
}
