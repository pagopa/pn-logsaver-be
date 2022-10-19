package it.pagopa.pn.logsaver.model.enums;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.apache.commons.collections4.SetUtils;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.model.LogFileReference.ClassifiedLogFragment;
import it.pagopa.pn.logsaver.services.impl.functions.LogProcessFunction;
import lombok.Getter;

@Getter
public enum LogFileType {


  CDC(Set.of(Retention.AUDIT10Y),
      (in, cfg) -> Stream.of(
          new ClassifiedLogFragment(Retention.AUDIT10Y, in.getContent(), in.getFileName()))), LOGS(
              Set.of(Retention.values()), new LogProcessFunction());


  private Set<Retention> retentions;
  private BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> filter;



  private LogFileType(Set<Retention> retentions,
      BiFunction<LogFileReference, DailyContextCfg, Stream<ClassifiedLogFragment>> filter) {
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


  public Stream<ClassifiedLogFragment> filter(DailyContextCfg ctx, LogFileReference item) {
    return filter.apply(item, ctx);
  }
}
