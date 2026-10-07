package it.pagopa.pn.logsaver.services.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.pagopa.pn.logsaver.model.enums.Retention;
import net.logstash.logback.encoder.LogstashEncoder;

class LogsFilterSupportWarnTest {

  private static final Set<Retention> ALL = Set.of(Retention.values());
  private static final String LOG_GROUP = "/aws/batch/pn-logsaver-be";
  private static final String LOG_STREAM = "pn-logsaver-be-logs/default/0a1b2c3d";
  private static final String EVENT_ID = "38545123456789012345678901234567890123456789012345678901";
  private static final long EVENT_TIMESTAMP = 1759528020000L;
  private static final String TAIL_MARKER = "TAIL_MARKER";
  private static final int MAX_WARN_LENGTH = 1024;
  private static final int AWSLOGS_SPLIT_THRESHOLD = 16 * 1024;

  private Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void setUp() {
    logger = (Logger) LoggerFactory.getLogger(LogsFilterSupport.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  @Test
  void groupByRetention_shouldWarn_whenLogEventMessageIsMalformed() {
    JsonObject event = new JsonObject();
    event.addProperty("message", "not-a-json-object");
    JsonArray events = new JsonArray();
    events.add(event);
    JsonObject parent = new JsonObject();
    parent.add("logEvents", events);

    LogsFilterSupport.groupByRetention(parent, Set.of(Retention.values()));

    boolean warned = appender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN);
    assertTrue(warned, "atteso un WARN sul messaggio malformato (declassamento non silenzioso)");
  }

  @Test
  void groupByRetention_shouldWarnWithoutPayload_whenMessageIsLargeFragment() {
    String fragment = largeFragment(300_000);

    LogsFilterSupport.groupByRetention(parentWith(event(fragment)), ALL);

    ILoggingEvent warn = singleWarn();
    String msg = warn.getFormattedMessage();
    assertTrue(msg.length() < MAX_WARN_LENGTH, "WARN troppo grande: " + msg.length());
    assertTrue(msg.startsWith("Error parsing log event message - unknown format"));
    assertTrue(msg.contains("logGroup=" + LOG_GROUP));
    assertTrue(msg.contains("logStream=" + LOG_STREAM));
    assertTrue(msg.contains("id=" + EVENT_ID));
    assertTrue(msg.contains("timestamp=" + EVENT_TIMESTAMP));
    assertTrue(msg.contains("messageLength=300000"));
    assertFalse(msg.contains(TAIL_MARKER), "il WARN non deve contenere il payload completo");
    assertNull(warn.getThrowableProxy(), "l'eccezione non va allegata al WARN");
  }

  @Test
  void groupByRetention_shouldWarn_whenMessageIsMissing() {
    JsonObject event = new JsonObject();
    event.addProperty("id", EVENT_ID);

    Map<Retention, JsonObject> grouped = LogsFilterSupport.groupByRetention(parentWith(event), ALL);

    assertTrue(singleWarn().getFormattedMessage().contains("messageLength=-1"));
    assertEquals(1, grouped.get(Retention.DEVELOPER).getAsJsonArray("logEvents").size());
  }

  @Test
  void groupByRetention_shouldNotWarnAgain_whenReprocessingItsOwnWarn() {
    LogsFilterSupport.groupByRetention(parentWith(event(largeFragment(300_000))), ALL);
    String stdoutLine = encodeAsLogstashLine(singleWarn());
    assertTrue(stdoutLine.length() < AWSLOGS_SPLIT_THRESHOLD,
        "la riga di WARN verrebbe spezzata da awslogs: " + stdoutLine.length());

    appender.list.clear();
    Map<Retention, JsonObject> grouped =
        LogsFilterSupport.groupByRetention(parentWith(event(stdoutLine)), ALL);

    assertTrue(warns().isEmpty(), "la rielaborazione del WARN non deve generare un nuovo WARN");
    assertEquals(1, grouped.get(Retention.DEVELOPER).getAsJsonArray("logEvents").size());
  }

  private JsonObject event(String message) {
    JsonObject e = new JsonObject();
    e.addProperty("id", EVENT_ID);
    e.addProperty("timestamp", EVENT_TIMESTAMP);
    e.addProperty("message", message);
    return e;
  }

  private JsonObject parentWith(JsonObject event) {
    JsonObject parent = new JsonObject();
    parent.addProperty("messageType", "DATA_MESSAGE");
    parent.addProperty("logGroup", LOG_GROUP);
    parent.addProperty("logStream", LOG_STREAM);
    JsonArray logEvents = new JsonArray();
    logEvents.add(event);
    parent.add("logEvents", logEvents);
    return parent;
  }

  private String largeFragment(int length) {
    StringBuilder sb = new StringBuilder("\" a meta' di un JSON\",\"sourceIPAddress\":\"10.4.10.38\",");
    String chunk = "\\\"eventSource\\\":\\\"s3.amazonaws.com\\\",";
    while (sb.length() < length) {
      sb.append(chunk);
    }
    sb.setLength(length - TAIL_MARKER.length());
    return sb.append(TAIL_MARKER).toString();
  }

  private List<ILoggingEvent> warns() {
    return appender.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
  }

  private ILoggingEvent singleWarn() {
    List<ILoggingEvent> warns = warns();
    assertEquals(1, warns.size());
    return warns.get(0);
  }

  private String encodeAsLogstashLine(ILoggingEvent evt) {
    LogstashEncoder encoder = new LogstashEncoder();
    encoder.setContext(logger.getLoggerContext());
    encoder.start();
    try {
      return new String(encoder.encode(evt), StandardCharsets.UTF_8).trim();
    } finally {
      encoder.stop();
    }
  }
}
