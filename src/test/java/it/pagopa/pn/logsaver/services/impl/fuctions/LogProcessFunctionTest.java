package it.pagopa.pn.logsaver.services.impl.fuctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.google.gson.JsonStreamParser;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.exceptions.LogFilterException;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.Retention;
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
    assertEquals(3, ret.size());

    assertEquals(1, filterResult(ret, Retention.AUDIT10Y).size());
    assertEquals(1, filterResult(ret, Retention.AUDIT5Y).size());
    assertEquals(1, filterResult(ret, Retention.DEVELOPER).size());

    assertEquals(List.of(4, 4), logEventSizes(ret, Retention.AUDIT10Y));
    assertEquals(List.of(1, 1), logEventSizes(ret, Retention.AUDIT5Y));
    assertEquals(List.of(19, 9, 2, 14, 4, 4), logEventSizes(ret, Retention.DEVELOPER));

    ret.forEach(fragment -> assertEquals(item.getFileName(), fragment.getFileName()));
  }

  @Test
  void apply_shouldCloseSourceStream_afterStreamConsumedAndClosed() throws IOException {
    when(ctx.retentions()).thenReturn(Set.of(Retention.values()));
    InputStream sourceSpy = spy(s3File.getInputStream());
    LogFileReference item = LogFileReference.builder().logDate(TestCostant.LOGDATE)
        .s3Key(TestCostant.S3_KEY).content(sourceSpy).build();

    try (Stream<ClassifiedLogFragment> result = function.apply(item, ctx)) {
      result.forEach(fragment -> {
      });
    }

    verify(sourceSpy, atLeastOnce()).close();
  }

  private List<ClassifiedLogFragment> filterResult(List<ClassifiedLogFragment> ret, Retention retention) {
    return ret.stream().filter(item -> item.getRetention() == retention)
        .collect(Collectors.toList());

  }

  private List<Integer> logEventSizes(List<ClassifiedLogFragment> ret, Retention retention) {
    InputStream content = filterResult(ret, retention).get(0).getContent();
    List<Integer> sizes = new ArrayList<>();
    try {
      JsonStreamParser parser =
          new JsonStreamParser(IOUtils.toString(content, StandardCharsets.UTF_8));
      while (parser.hasNext()) {
        sizes.add(parser.next().getAsJsonObject().getAsJsonArray("logEvents").size());
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return sizes;
  }
}
