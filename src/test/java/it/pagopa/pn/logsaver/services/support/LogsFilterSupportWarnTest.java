package it.pagopa.pn.logsaver.services.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

class LogsFilterSupportWarnTest {

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
}
