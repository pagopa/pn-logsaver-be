package it.pagopa.pn.logsaver.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import it.pagopa.pn.logsaver.model.ArchiveInfo;
import it.pagopa.pn.logsaver.model.ItemLog;
import it.pagopa.pn.logsaver.model.LogType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogProcessorServiceImpl {

  @NonNull
  private final LogSaverCfg cfg;
  @NonNull
  private final LogReaderService s3Service;

  private Map<Retention, Path> retentionMap;

  private Path tempFullPath;

  private Path tempUnzipPath;

  @PostConstruct
  void initTmpFolders() {

    this.tempFullPath = Paths.get(cfg.getTempBasePath(), cfg.getLogDate().toString());
    String tmpDailyPathStr = tempFullPath.toString();
    this.retentionMap =
        Map.of(Retention.AUDIT10Y, Paths.get(tmpDailyPathStr, Retention.AUDIT10Y.getFolder()),
            Retention.AUDIT5Y, Paths.get(tmpDailyPathStr, Retention.AUDIT5Y.getFolder()),
            Retention.AUDIT2Y, Paths.get(tmpDailyPathStr, Retention.AUDIT2Y.getFolder()));

    this.tempUnzipPath = Path.of(tempFullPath.toString(), "uziplog");

    FilesUtils.createOrCleanDirectory(tempFullPath);
    FilesUtils.createDirectories(retentionMap.values());
    FilesUtils.createDirectory(tempUnzipPath);
  }

  @PreDestroy
  public void destroyTmpFolders() {
    FilesUtils.remove(tempFullPath);
  }

  public ItemLog temporaryStore(ItemLog log) {

    InputStream content = s3Service.getLogContent(log.getS3Key());
    String fileName = FilenameUtils.getBaseName(log.getS3Key());

    if (LogType.cdc == log.getType()) {
      writeLog(content, fileName, Retention.AUDIT10Y);
    } else {

      // TODO Decidere se utilizzare iostream:
      // decomprimere su disco OutputStream
      // Parsing json da disco InputStream

      try {
        File tempUnzipedFile = File.createTempFile(fileName, "tmp", tempUnzipPath.toFile());
        FileOutputStream fileOut = new FileOutputStream(tempUnzipedFile, true);

        FilesUtils.decompressGzipToBytes(content, fileOut);

        parseAndWriteLog(new FileInputStream(tempUnzipedFile), fileName);

        tempUnzipedFile.deleteOnExit();

      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }



      // byte[] unziped = FilesUtils.decompressGzipToBytes(content);

      // parseAndWriteLog(new ByteArrayInputStream(unziped), fileName);



    }

    return log;
  }


  private void parseAndWriteLog(InputStream content, String fileName) {

    try (Reader reader = new InputStreamReader(content)) {

      Iterator<JsonElement> sourceIterator = new JsonStreamParser(reader);

      // TODO valutare se consumare lo stream come Flux reactor
      Stream<JsonElement> targetStream =
          StreamSupport.stream(((Iterable<JsonElement>) () -> sourceIterator).spliterator(), false);

      targetStream.map(JsonElement::toString)
          .forEach(item -> writeLog(new ByteArrayInputStream(item.getBytes()), fileName,
              getRetention(item)));


    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }


  }

  private void writeLog(InputStream content, String fileName, Retention retention) {
    if (Objects.nonNull(retention)) {
      Path path = retentionMap.get(retention);
      FilesUtils.writeFile(content, fileName, path);
    }
  }

  public List<ArchiveInfo> zipAllItemsByRetention() {
    return retentionMap.entrySet().stream().map(this::createZipFile).collect(Collectors.toList());
  }

  private ArchiveInfo createZipFile(Entry<Retention, Path> entry) {
    Path fileZipOut = Path.of(tempFullPath.toString(), entry.getKey().name() + ".zip");
    FilesUtils.zipDirectory(entry.getValue(), fileZipOut);
    return ArchiveInfo.builder().filePath(fileZipOut).logDate(cfg.getLogDate())
        .retention(entry.getKey()).build();
  }



  private Retention getRetention(String jsonStr) {
    // TODO Decidere come discriminare la retention

    // String jsonStr = jsonLog.toPrettyString();

    Retention ret = Stream.of(Retention.values())
        .filter(retention -> jsonStr.contains(retention.name())).findFirst().orElse(null);

    return ret;
  }
  /*
   * if (jsonStr.contains(Retention.AUDIT10Y.name())) { return Retention.AUDIT10Y; } else if
   * (jsonStr.contains(Retention.AUDIT5Y.name())) { return Retention.AUDIT5Y; } else if
   * (jsonStr.contains(Retention.AUDIT2Y.name())) { return Retention.AUDIT2Y; } /* List<JsonNode>
   * logsEvent = jsonLog.findValues("logEvents"); logsEvent.stream().forEach(logEvt -> {
   * logEvt.findValuesAsText("message").stream().forEach(null)
   * 
   * });
   */



}
