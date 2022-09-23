package it.pagopa.pn.logsaver.springbootcfg;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.impl.LogProcessFunction;
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


  @PostConstruct
  private void initFilters() {
    itemFilterFunction.put(ItemType.CDC,
        (in, cfg) -> Stream.of(new ItemChildren(Retention.AUDIT10Y, in)));
    itemFilterFunction.put(ItemType.LOGS, new LogProcessFunction());
  }

  public Stream<ItemChildren> filter(ItemType type, DailyContextCfg ctx, InputStream content) {
    return itemFilterFunction.get(type).apply(content, ctx);
  }


}
