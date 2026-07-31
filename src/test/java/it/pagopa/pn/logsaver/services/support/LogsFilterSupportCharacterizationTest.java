package it.pagopa.pn.logsaver.services.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.pagopa.pn.logsaver.model.enums.Retention;

class LogsFilterSupportCharacterizationTest {

  private static final Set<Retention> ALL = Set.of(Retention.values());


  private JsonObject event(String message) {
    JsonObject e = new JsonObject();
    e.addProperty("message", message);
    return e;
  }

  private String msgWithTags(String... tags) {
    JsonObject m = new JsonObject();
    JsonArray arr = new JsonArray();
    for (String t : tags) {
      arr.add(t);
    }
    m.add("tags", arr);
    return m.toString();
  }

  private JsonObject parentWith(JsonObject... events) {
    JsonObject parent = new JsonObject();
    parent.addProperty("owner", "test-owner");
    JsonArray logEvents = new JsonArray();
    for (JsonObject e : events) {
      logEvents.add(e);
    }
    parent.add("logEvents", logEvents);
    return parent;
  }

  private int eventsIn(Map<Retention, JsonObject> grouped, Retention retention) {
    JsonObject obj = grouped.get(retention);
    return obj == null ? -1 : obj.getAsJsonArray("logEvents").size();
  }


  @Test
  @DisplayName("Tag audit noti -> mappati alla rispettiva retention (retention tutte esportabili)")
  void auditTagsMappedToRetention() {
    JsonObject parent = parentWith(
        event(msgWithTags(Retention.AUDIT10Y.name())),
        event(msgWithTags(Retention.AUDIT5Y.name())),
        event(msgWithTags(Retention.AUDIT2Y.name())));

    Map<Retention, JsonObject> grouped = LogsFilterSupport.groupByRetention(parent, ALL);

    assertEquals(1, eventsIn(grouped, Retention.AUDIT10Y));
    assertEquals(1, eventsIn(grouped, Retention.AUDIT5Y));
    assertEquals(1, eventsIn(grouped, Retention.AUDIT2Y));
    assertFalse(grouped.containsKey(Retention.DEVELOPER));
  }

  @Test
  @DisplayName("Precedenza: evento con piu' tag audit -> vince AUDIT10Y (controllato per primo)")
  void multipleAuditTagsPreferHighest() {
    JsonObject parent = parentWith(event(msgWithTags(
        Retention.AUDIT2Y.name(), Retention.AUDIT5Y.name(), Retention.AUDIT10Y.name())));

    Map<Retention, JsonObject> grouped = LogsFilterSupport.groupByRetention(parent, ALL);

    assertEquals(1, eventsIn(grouped, Retention.AUDIT10Y));
    assertFalse(grouped.containsKey(Retention.AUDIT5Y));
    assertFalse(grouped.containsKey(Retention.AUDIT2Y));
  }

  @Test
  @DisplayName("FROZEN R2: evento senza tag o con message malformato -> DEVELOPER (se esportabile)")
  void untaggedFallsBackToDeveloper() {
    JsonObject parent = parentWith(
        event(msgWithTags()),
        event("not-a-json-message"));

    Map<Retention, JsonObject> grouped = LogsFilterSupport.groupByRetention(parent, ALL);

    assertEquals(2, eventsIn(grouped, Retention.DEVELOPER));
    assertFalse(grouped.containsKey(Retention.AUDIT10Y));
  }

  @Test
  @DisplayName("FROZEN R2: evento senza tag -> SCARTATO in silenzio se DEVELOPER non e' esportabile")
  void untaggedDroppedWhenDeveloperNotExported() {
    JsonObject parent = parentWith(event(msgWithTags()));

    Map<Retention, JsonObject> grouped =
        LogsFilterSupport.groupByRetention(parent, Set.of(Retention.AUDIT10Y));

    assertTrue(grouped.isEmpty());
  }

  @Test
  @DisplayName("FROZEN R2: evento taggato AUDIT10Y ma 10Y non esportabile -> declassato a DEVELOPER")
  void audit10YDowngradedWhenNotExported() {
    JsonObject parent = parentWith(event(msgWithTags(Retention.AUDIT10Y.name())));

    Map<Retention, JsonObject> grouped =
        LogsFilterSupport.groupByRetention(parent, Set.of(Retention.AUDIT5Y, Retention.DEVELOPER));

    assertEquals(1, eventsIn(grouped, Retention.DEVELOPER));
    assertFalse(grouped.containsKey(Retention.AUDIT10Y));
  }
}
