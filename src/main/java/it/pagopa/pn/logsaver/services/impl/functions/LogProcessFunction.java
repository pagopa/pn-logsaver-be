package it.pagopa.pn.logsaver.services.impl.functions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import it.pagopa.pn.logsaver.exceptions.LogFilterException;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.services.support.LogsFilterSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class LogProcessFunction implements BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> {

  @Override
  public Stream<ClassifiedLogFragment> apply(LogFileReference logFileRef, DailyContextCfg ctx) {
    try (Reader reader = new InputStreamReader(new GZIPInputStream(logFileRef.getContent()))) {

      Iterator<JsonElement> sourceIterator = new JsonStreamParser(reader);

      Stream<JsonElement> targetStream =
          StreamSupport.stream(((Iterable<JsonElement>) () -> sourceIterator).spliterator(), false);

      Map<Retention, ByteArrayOutputStream> contentByRetention = new LinkedHashMap<>();

      targetStream.map(JsonElement::getAsJsonObject)
          .map(json -> LogsFilterSupport.groupByRetention(json, ctx.retentions()))
          .forEach(byRetention -> byRetention.forEach((retention, logToWrite) -> appendRecord(
              contentByRetention.computeIfAbsent(retention, ret -> new ByteArrayOutputStream()),
              logToWrite.toString())));

      return contentByRetention.entrySet().stream()
          .map(entryRetentionAudit -> new ClassifiedLogFragment(entryRetentionAudit.getKey(),
              new ByteArrayInputStream(entryRetentionAudit.getValue().toByteArray()),
              logFileRef.getFileName()));

    } catch (Exception e) {
      log.error("Log filtering error. The content of the file is not valid json-stream: {}",
          e.getMessage());
      throw new LogFilterException("Filter error. The content of the file is not valid json-stream",
          e);
    }
  }

  private void appendRecord(ByteArrayOutputStream target, String logToWrite) {
    try {
      target.write(logToWrite.getBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
