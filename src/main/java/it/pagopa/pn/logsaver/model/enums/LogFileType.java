package it.pagopa.pn.logsaver.model.enums;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.IOUtils;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.services.impl.functions.LogProcessFunction;
import lombok.Getter;

@Getter
public enum LogFileType {


  CDC(Set.of(Retention.AUDIT10Y),
      (in, content, cfg) -> Stream.of(new ClassifiedLogFragment(Retention.AUDIT10Y,
          readContent(in, content), in.getFileName()))), LOGS(
              Set.of(Retention.values()), new LogProcessFunction());


  @FunctionalInterface
  public interface LogFilter {
    Stream<ClassifiedLogFragment> apply(LogFileReference item, InputStream content,
        DailyContextCfg ctx);
  }

  private Set<Retention> retentions;
  private LogFilter filter;



  private LogFileType(Set<Retention> retentions, LogFilter filter) {
    this.retentions = retentions;
    this.filter = filter;

  }

  public static List<String> valuesAsString() {
    return IEnum.valuesAsString(LogFileType.class);
  }

  public static List<String> valuesAsString(Collection<LogFileType> list) {
    return IEnum.valuesAsString(list);
  }

  public static Set<LogFileType> values(List<String> list) {
    return IEnum.values(list, LogFileType.class);
  }

  public boolean containsRetentions(Set<Retention> tocheck) {
    return !SetUtils.intersection(retentions, tocheck).isEmpty();
  }


  public Stream<ClassifiedLogFragment> filter(DailyContextCfg ctx, LogFileReference item,
      InputStream content) {
    return filter.apply(item, content, ctx);
  }

  private static byte[] readContent(LogFileReference item, InputStream content) {
    try {
      return IOUtils.toByteArray(content);
    } catch (IOException e) {
      throw new UncheckedIOException("Error reading log file " + item.getS3Key(), e);
    }
  }
}
