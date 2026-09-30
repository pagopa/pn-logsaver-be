package it.pagopa.pn.logsaver.model.enums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.utils.LogSaverUtils;

class LogFileTypeCdcFilterTest {

  private static final byte[] BODY =
      "{\"tableName\":\"pn-Mandate\",\"eventID\":\"cdc-1\"}".getBytes(StandardCharsets.UTF_8);

  private DailyContextCfg ctx() {
    return DailyContextCfg.builder()
        .retentionExportTypeMap(LogSaverUtils.defaultRetentionExportTypeMap())
        .tmpBasePath(TestCostant.TMP_FOLDER).logFileTypes(Set.of(LogFileType.values()))
        .logDate(TestCostant.LOGDATE).build();
  }

  private LogFileReference cdcItem() {
    return LogFileReference.builder().logDate(TestCostant.LOGDATE).type(LogFileType.CDC)
        .s3Key(TestCostant.S3_KEY).build();
  }

  @Test
  void cdcFilter_readsContentFromParameter_andClassifiesAsAudit10y() {
    LogFileReference item = cdcItem();
    InputStream content = new ByteArrayInputStream(BODY);

    List<ClassifiedLogFragment> fragments;
    try (Stream<ClassifiedLogFragment> stream = LogFileType.CDC.filter(ctx(), item, content)) {
      fragments = stream.toList();
    }

    assertEquals(1, fragments.size());
    ClassifiedLogFragment fragment = fragments.get(0);
    assertEquals(Retention.AUDIT10Y, fragment.getRetention());
    assertArrayEquals(BODY, fragment.getContent());
    assertEquals(item.getFileName(), fragment.getFileName());
  }

  @Test
  void cdcFilter_readsFromTheGivenStream_notFromAnyStateOnTheReference() {
    LogFileReference item = cdcItem();
    byte[] otherBody = "{\"eventID\":\"cdc-2\"}".getBytes(StandardCharsets.UTF_8);

    List<ClassifiedLogFragment> fragments;
    try (Stream<ClassifiedLogFragment> stream =
        LogFileType.CDC.filter(ctx(), item, new ByteArrayInputStream(otherBody))) {
      fragments = stream.toList();
    }

    assertEquals(1, fragments.size());
    assertArrayEquals(otherBody, fragments.get(0).getContent());
  }

  @Test
  void cdcFilter_wrapsReadFailure_reportingTheS3Key() {
    LogFileReference item = cdcItem();
    InputStream failing = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("boom");
      }
    };

    DailyContextCfg ctx = ctx();

    UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
        () -> LogFileType.CDC.filter(ctx, item, failing));

    assertTrue(thrown.getMessage().contains(TestCostant.S3_KEY));
  }
}
