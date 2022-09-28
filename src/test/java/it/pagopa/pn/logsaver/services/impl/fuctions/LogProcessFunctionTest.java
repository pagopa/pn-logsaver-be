package it.pagopa.pn.logsaver.services.impl.fuctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import it.pagopa.pn.logsaver.exceptions.LogFilterException;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.impl.functions.LogProcessFunction;

@ExtendWith(SpringExtension.class)
class LogProcessFunctionTest {
  private final String fileLog =
      "classpath:files/s3/pn-pnDelivery-ecs-delivery-stream-1-2022-07-12-00-05-07-ed57bcd0-ce62-4566-a943-04f9e462e54c";

  @Value(fileLog)
  private Resource s3File;

  @Mock
  private DailyContextCfg ctx;


  private LogProcessFunction function;

  @BeforeEach
  void setUp() {
    this.function = new LogProcessFunction();

  }

  @Test
  void filter_InputStreamMalformed() throws IOException {

    InputStream in = IOUtils.toInputStream("test");
    assertThrows(LogFilterException.class, () -> function.apply(in, ctx));
  }


  @Test
  void filter() throws IOException {
    when(ctx.retentions()).thenReturn(Set.of(Retention.values()));

    List<ItemChildren> ret =
        function.apply(s3File.getInputStream(), ctx).sequential().collect(Collectors.toList());

    assertNotNull(ret);
    assertEquals(10, ret.size());

    assertEquals(2, filterResult(ret, Retention.AUDIT10Y).size());
    assertEquals(2, filterResult(ret, Retention.AUDIT5Y).size());
    assertEquals(6, filterResult(ret, Retention.DEVELOPER).size());

    List<JsonArray> logEvt10List = filterResult(ret, Retention.AUDIT10Y).stream()
        .map(ItemChildren::getContent).map(this::getLogEvent).collect(Collectors.toList());

    List<JsonArray> logEvt5List = filterResult(ret, Retention.AUDIT5Y).stream()
        .map(ItemChildren::getContent).map(this::getLogEvent).collect(Collectors.toList());

    List<JsonArray> logEvtDevList = filterResult(ret, Retention.DEVELOPER).stream()
        .map(ItemChildren::getContent).map(this::getLogEvent).collect(Collectors.toList());

    assertEquals(4, logEvt10List.get(0).size());
    assertEquals(4, logEvt10List.get(1).size());

    assertEquals(1, logEvt5List.get(0).size());
    assertEquals(1, logEvt5List.get(1).size());

    assertEquals(19, logEvtDevList.get(0).size());
    assertEquals(9, logEvtDevList.get(1).size());
    assertEquals(2, logEvtDevList.get(2).size());
    assertEquals(14, logEvtDevList.get(3).size());
    assertEquals(4, logEvtDevList.get(4).size());
    assertEquals(4, logEvtDevList.get(5).size());

  }

  private List<ItemChildren> filterResult(List<ItemChildren> ret, Retention retention) {
    return ret.stream().filter(item -> item.getRetention() == retention)
        .collect(Collectors.toList());

  }

  private JsonArray getLogEvent(InputStream in) {
    JsonObject jsonObject;
    try {
      jsonObject = JsonParser.parseString(IOUtils.toString(in)).getAsJsonObject();
    } catch (JsonSyntaxException | IOException e) {
      return null;
    }
    return jsonObject.getAsJsonArray("logEvents");
  }
}