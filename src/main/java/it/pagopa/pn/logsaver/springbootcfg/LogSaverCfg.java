package it.pagopa.pn.logsaver.springbootcfg;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.model.ItemType;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.LogProcessFunction;
import lombok.Getter;


@Configuration
@Getter
public class LogSaverCfg {

  private LocalDate logDate2;

  @Value("${log-parser.tmp-folder}")
  private String tmpBasePath;

  // private Map<Retention, Path> retentionTmpPath = new LinkedHashMap<>();

  // private Path tmpDailyPath;

  private Map<ItemType, BiFunction<InputStream, DailyContextCfg, Stream<ItemChildren>>> itemFilterFunction =
      new LinkedHashMap<>();


  /*
   * @Autowired private void setLogDate(@Value("${logDate}") String date) { logDate =
   * DateUtils.parse(date); if (Objects.isNull(logDate)) { logDate = DateUtils.yesterday(); } }
   */



  @Autowired
  private void initFilter() { //
    itemFilterFunction.put(ItemType.cdc,
        (in, cfg) -> Stream.of(new ItemChildren(Retention.AUDIT10Y, in)));
    itemFilterFunction.put(ItemType.logs, new LogProcessFunction());
  }

  public BiFunction<InputStream, DailyContextCfg, Stream<ItemChildren>> getFilterFunction(Item item) {
    return itemFilterFunction.get(item.getType());
  }



  @PostConstruct
  private void initConfiguration() {

  }



  @PreDestroy
  public void destroy() {

  }

}
