package it.pagopa.pn.logsaver.utils;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import it.pagopa.pn.logsaver.model.Retention;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JsonUtils {

  private final static String FIELD_LOG_EVENTS = "logEvents";
  private final static String FIELD_LOG_MESSAGE = "message";
  private final static String FIELD_LOG_TAGS = "tags";

  public static Map<Retention, JsonObject> groupByRetention(JsonObject parent) {
    Map<Retention, JsonObject> groupedLog = new LinkedHashMap<>();

    JsonArray logEvents = parent.getAsJsonArray(FIELD_LOG_EVENTS);
    if (Objects.nonNull(logEvents)) {
      logEvents
          .forEach(logEvent -> splitLogByRetention(parent, groupedLog, logEvent.getAsJsonObject()));
    }

    return groupedLog;
  }

  private static void splitLogByRetention(JsonObject parent, Map<Retention, JsonObject> groupedLog,
      JsonObject logEvent) {
    try {
      Retention retention = getRetention(logEvent);
      JsonObject objByRetention =
          groupedLog.computeIfAbsent(retention, ret -> createEmptyLog(parent.getAsJsonObject()));
      objByRetention.getAsJsonArray(FIELD_LOG_EVENTS).add(logEvent);
    } catch (Exception e) {
      log.warn("error parsing log events");
    }

  }

  private static Retention getRetention(JsonElement logEvt) {

    try {
      String logEvtMsgStr =
          logEvt.getAsJsonObject().getAsJsonPrimitive(FIELD_LOG_MESSAGE).getAsString();

      JsonObject logEvtMsg = JsonParser.parseString(logEvtMsgStr).getAsJsonObject();

      Type listType = new TypeToken<List<String>>() {}.getType();
      JsonArray tags = logEvtMsg.getAsJsonArray(FIELD_LOG_TAGS);

      if (Objects.nonNull(tags) && !tags.isEmpty()) {

        List<String> strList = new Gson().fromJson(tags, listType);

        if (strList.contains(Retention.AUDIT10Y.name())) {
          return Retention.AUDIT10Y;
        } else if (strList.contains(Retention.AUDIT5Y.name())) {
          return Retention.AUDIT5Y;
        }
      }
    } catch (Throwable e) {
      log.trace("error parsing log event message unknow format: " + logEvt.toString());
    }
    return Retention.DEVELOPER;

  }

  private static JsonObject createEmptyLog(JsonObject jsonEl) {
    JsonObject template = jsonEl.deepCopy();
    template.add(FIELD_LOG_EVENTS, new JsonArray());
    return template;
  }



}
