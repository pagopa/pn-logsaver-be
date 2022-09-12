package it.pagopa.pn.logsaver.services;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.model.ItemLog;
import it.pagopa.pn.logsaver.model.LogType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.S3Object;



@Service
@AllArgsConstructor
@Slf4j
public class LogReaderServiceImpl implements LogReaderService {

  private final S3BucketClient clientS3;


  @Override
  public List<ItemLog> findLogs(LogType type, LocalDate date) {

    final List<ItemLog> ret = new ArrayList<>();

    List<String> apps = clientS3.findSubFolders(type.getSubFolfer());
    apps.stream().forEach(appName -> handleItems(ret, appName, type, date));
    return ret;
  }

  @Override
  public List<ItemLog> findLogs(LocalDate date) {

    final List<ItemLog> ret = new ArrayList<>();

    Stream.of(LogType.values()).flatMap(type -> findLogs(type, date).stream())
        .collect(Collectors.toCollection(() -> ret));

    return ret;
  }

  @Override
  public InputStream getLogContent(String key) {
    return clientS3.getObjectContent(key);
  }


  private void handleItems(List<ItemLog> itemList, String appName, LogType type, LocalDate date) {

    String prefix = handleDailyPrefix(appName, type, date);
    List<S3Object> objList = clientS3.findObjects(prefix);
    objList.stream().map(obj -> ItemLog.builder().s3Key(obj.key()).type(type).logDate(date).build())
        .collect(Collectors.toCollection(() -> itemList));

  }

  private String handleDailyPrefix(String appName, LogType type, LocalDate date) {

    String dateSuffix = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    return String.format(type.getDailyFolferPattern(), type.name(), appName, dateSuffix);
  }

}
