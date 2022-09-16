package it.pagopa.pn.logsaver.springbootcfg;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.services.impl.LogProcessFunction;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import lombok.Getter;


@Configuration
@Getter
public class LogSaverCfg {


  @Value("${log-saver.tmp-folder}")
  private String tmpBasePath;

  @Value("${log-saver.logs-root-path-template}")
  private String logsRootPathTemplate;

  @Value("${log-saver.logs-microservice}")
  private List<String> logsMicroservice;

  @Value("${log-saver.cdc-root-path-template}")
  private String cdcRootPathTemplate;

  @Value("${log-saver.cdc-tables:}")
  private List<String> cdcTables;


  private Map<ItemType, BiFunction<InputStream, DailyContextCfg, Stream<ItemChildren>>> itemFilterFunction =
      new LinkedHashMap<>();


  @Autowired
  private void initFilters() {
    itemFilterFunction.put(ItemType.cdc,
        (in, cfg) -> Stream.of(new ItemChildren(Retention.AUDIT10Y, in)));
    itemFilterFunction.put(ItemType.logs, new LogProcessFunction());
  }

  public BiFunction<InputStream, DailyContextCfg, Stream<ItemChildren>> getFilterFunction(
      Item item) {
    return itemFilterFunction.get(item.getType());
  }



  @PostConstruct
  private void initConfiguration() {}



  @PreDestroy
  public void destroy() {

  }

}
