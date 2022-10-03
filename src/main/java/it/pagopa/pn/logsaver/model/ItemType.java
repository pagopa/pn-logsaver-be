package it.pagopa.pn.logsaver.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.apache.commons.collections4.SetUtils;
import it.pagopa.pn.logsaver.model.Item.ItemChildren;
import it.pagopa.pn.logsaver.services.impl.functions.LogProcessFunction;
import lombok.Getter;

@Getter
public enum ItemType {


  CDC("cdc/", Set.of(Retention.AUDIT10Y),
      (in, cfg) -> Stream
          .of(new ItemChildren(Retention.AUDIT10Y, in.getContent(), in.getFileName()))), LOGS(
              "logs/ecs/", Set.of(Retention.values()), new LogProcessFunction());



  private String subFolfer;
  private Set<Retention> retentions;
  private BiFunction<Item, DailyContextCfg, Stream<ItemChildren>> filter;



  private ItemType(String subFolfer, Set<Retention> retentions,
      BiFunction<Item, DailyContextCfg, Stream<ItemChildren>> filter) {
    this.subFolfer = subFolfer;
    this.retentions = retentions;
    this.filter = filter;

  }

  public static List<String> valuesAsString() {
    return IEnum.valuesAsString(ItemType.class);
  }

  public static List<String> valuesAsString(Collection<ItemType> list) {
    return IEnum.valuesAsString(list);
  }

  public static Set<ItemType> values(List<String> list) {
    return IEnum.values(list, ItemType.class);
  }

  public boolean containsRetentions(Set<Retention> tocheck) {
    return !SetUtils.intersection(retentions, tocheck).isEmpty();
  }


  public Stream<ItemChildren> filter(DailyContextCfg ctx, Item item) {
    return filter.apply(item, ctx);
  }
}
