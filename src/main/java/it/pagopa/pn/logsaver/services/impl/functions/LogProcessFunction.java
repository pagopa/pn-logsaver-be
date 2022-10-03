package it.pagopa.pn.logsaver.services.impl.functions;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import it.pagopa.pn.logsaver.exceptions.LogFilterException;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.services.support.LogsFilterSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class LogProcessFunction implements BiFunction<Item, DailyContextCfg, Stream<ItemChildren>> {

  @Override
  public Stream<ItemChildren> apply(Item item, DailyContextCfg ctx) {
    try {

      Reader reader = new InputStreamReader(new GZIPInputStream(item.getContent()));
      Iterator<JsonElement> sourceIterator = new JsonStreamParser(reader);

      Stream<JsonElement> targetStream =
          StreamSupport.stream(((Iterable<JsonElement>) () -> sourceIterator).spliterator(), false);


      return targetStream.map(JsonElement::getAsJsonObject)
          .map(json -> LogsFilterSupport.groupByRetention(json, ctx.retentions()))
          .map(Map::entrySet).flatMap(Set::stream).map(entryRetentionAudit -> {
            String logToWrite = entryRetentionAudit.getValue().toString();
            Retention retention = entryRetentionAudit.getKey();
            return new ItemChildren(retention, new ByteArrayInputStream(logToWrite.getBytes()),
                item.getFileName());
          });

    } catch (Exception e) {
      log.error("Log filtering error. The content of the file is not valid json-stream: {}",
          e.getMessage());
      throw new LogFilterException("Filter error. The content of the file is not valid json-stream",
          e);
    }
  }
}
