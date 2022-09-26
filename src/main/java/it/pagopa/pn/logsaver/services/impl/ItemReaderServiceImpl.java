package it.pagopa.pn.logsaver.services.impl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
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


  private Stream<Item> findItems(ItemType type, LocalDate date) {
    log.info("Start search {} items.", type.name());

    List<String> appsCfg = ItemType.CDC == type ? cfg.getCdcTables() : cfg.getLogsMicroservice();

    Stream<String> apps =
        appsCfg.isEmpty() ? clientS3.findSubFolders(type.getSubFolfer()) : appsCfg.stream();

    return apps.flatMap(appName -> handleItems(appName, type, date));
  }

  @Override
  public List<Item> findItems(DailyContextCfg dailyCtx) {

    final List<Item> ret =
        Stream.of(ItemType.values()).filter(type -> type.containsRetentions(dailyCtx.retentions()))
            .flatMap(type -> findItems(type, dailyCtx.logDate())).collect(Collectors.toList());
    log.info("Total items {}", ret.size());
    return ret;
  }

  @Override
  public InputStream getItemContent(String key) {
    return clientS3.getObjectContent(key);
  }


  private Stream<Item> handleItems(String appName, ItemType type, LocalDate date) {

    String prefix = handleDailyPrefix(appName, type, date);
    log.info("Search {} items for subfolder {}", type.name(), prefix);
    Stream<S3Object> objList = clientS3.findObjects(prefix);
    return objList.map(obj -> Item.builder().s3Key(obj.key()).type(type).logDate(date).build());
  }

  private String handleDailyPrefix(String appName, ItemType type, LocalDate date) {
    String dailyTmpPattern =
        ItemType.CDC == type ? cfg.getCdcRootPathTemplate() : cfg.getLogsRootPathTemplate();
    return String.format(date.format(DateTimeFormatter.ofPattern(dailyTmpPattern)), appName);
  }


}
