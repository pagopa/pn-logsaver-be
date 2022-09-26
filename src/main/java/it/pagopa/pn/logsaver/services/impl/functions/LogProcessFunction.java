package it.pagopa.pn.logsaver.services.impl.functions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class LogProcessFunction
    implements BiFunction<InputStream, DailyContextCfg, Stream<ItemChildren>> {


  @Override
  public Stream<ItemChildren> apply(InputStream content, DailyContextCfg ctx) {
    return filter(content, ctx);
  }

  private Stream<ItemChildren> filter(InputStream content, DailyContextCfg ctx) {

    try {

      Reader reader = new InputStreamReader(new GZIPInputStream(content));
      Iterator<JsonElement> sourceIterator = new JsonStreamParser(reader);

      Stream<JsonElement> targetStream =
          StreamSupport.stream(((Iterable<JsonElement>) () -> sourceIterator).spliterator(), false);


      return targetStream.map(JsonElement::getAsJsonObject)
          .map(json -> JsonUtils.groupByRetention(json, ctx.retentions())).map(Map::entrySet)
          .flatMap(Set::stream).map(item -> {
            String logToWrite = item.getValue().toString();
            return new ItemChildren(item.getKey(), new ByteArrayInputStream(logToWrite.getBytes()));
          });

    } catch (Exception e) {
      log.error("Log filtering error. The content of the file is not valid json-stream", e);
      throw new LogFilterException("Filter error. The content of the file is not valid json-stream",
          e);
    }

  }

}
