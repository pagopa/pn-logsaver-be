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
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.exceptions.LogFilterException;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.impl.functions.LogProcessFunction;

@ExtendWith(SpringExtension.class)
class LogProcessFunctionTest {


  @Value(TestCostant.FILE_LOG)
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
    LogFileReference item =
        LogFileReference.builder().logDate(TestCostant.LOGDATE).s3Key(TestCostant.S3_KEY).content(in).build();
    assertThrows(LogFilterException.class, () -> function.apply(item, ctx));
  }


  @Test
  void filter() throws IOException {
    when(ctx.retentions()).thenReturn(Set.of(Retention.values()));
    LogFileReference item = LogFileReference.builder().logDate(TestCostant.LOGDATE).s3Key(TestCostant.S3_KEY)
        .content(s3File.getInputStream()).build();
    List<ClassifiedLogFragment> ret = function.apply(item, ctx).sequential().collect(Collectors.toList());

    assertNotNull(ret);
    assertEquals(10, ret.size());

    assertEquals(2, filterResult(ret, Retention.AUDIT10Y).size());
    assertEquals(2, filterResult(ret, Retention.AUDIT5Y).size());
    assertEquals(6, filterResult(ret, Retention.DEVELOPER).size());

    List<JsonArray> logEvt10List = filterResult(ret, Retention.AUDIT10Y).stream()
        .map(ClassifiedLogFragment::getContent).map(this::getLogEvent).collect(Collectors.toList());

    List<JsonArray> logEvt5List = filterResult(ret, Retention.AUDIT5Y).stream()
        .map(ClassifiedLogFragment::getContent).map(this::getLogEvent).collect(Collectors.toList());

    List<JsonArray> logEvtDevList = filterResult(ret, Retention.DEVELOPER).stream()
        .map(ClassifiedLogFragment::getContent).map(this::getLogEvent).collect(Collectors.toList());

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

  private List<ClassifiedLogFragment> filterResult(List<ClassifiedLogFragment> ret, Retention retention) {
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
