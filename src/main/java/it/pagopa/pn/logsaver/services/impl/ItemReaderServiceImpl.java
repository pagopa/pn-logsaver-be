package it.pagopa.pn.logsaver.services.impl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.S3Object;



@Service
@AllArgsConstructor
@Slf4j
public class ItemReaderServiceImpl implements ItemReaderService {

  private final S3BucketClient clientS3;
  private final LogSaverCfg cfg;

  @Override
  public List<Item> findItems(ItemType type, LocalDate date) {
    log.info("Start search {} items.", type.name());
    final List<Item> ret = new ArrayList<>();

    List<String> apps = ItemType.cdc == type ? cfg.getCdcTables() : cfg.getLogsMicroservice();

    if (Objects.isNull(apps) || apps.isEmpty()) {
      apps = clientS3.findSubFolders(type.getSubFolfer());
    }

    apps.stream().forEach(appName -> handleItems(ret, appName, type, date));
    log.info("Found {} items.", ret.size());
    return ret;
  }

  @Override
  public List<Item> findItems(LocalDate date) {

    final List<Item> ret = new ArrayList<>();

    Stream.of(ItemType.values()).flatMap(type -> findItems(type, date).stream())
        .collect(Collectors.toCollection(() -> ret));
    log.info("Total items {}", ret.size());
    return ret;
  }

  @Override
  public InputStream getItemContent(String key) {
    return clientS3.getObjectContent(key);
  }


  private void handleItems(List<Item> itemList, String appName, ItemType type, LocalDate date) {

    String prefix = handleDailyPrefix(appName, type, date);
    log.info("Search {} items for subfolder {}", type.name(), prefix);
    List<S3Object> objList = clientS3.findObjects(prefix);
    objList.stream().map(obj -> Item.builder().s3Key(obj.key()).type(type).logDate(date).build())
        .collect(Collectors.toCollection(() -> itemList));

  }

  private String handleDailyPrefix(String appName, ItemType type, LocalDate date) {
    String dailyTmpPattern =
        ItemType.cdc == type ? cfg.getCdcRootPathTemplate() : cfg.getLogsRootPathTemplate();
    return String.format(date.format(DateTimeFormatter.ofPattern(dailyTmpPattern)), appName);
  }


}
