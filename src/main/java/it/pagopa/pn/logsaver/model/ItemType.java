package it.pagopa.pn.logsaver.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public enum ItemType {
  CDC("cdc/"), LOGS("logs/ecs/");

  private String subFolfer;

  private ItemType(String subFolfer) {
    this.subFolfer = subFolfer;

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

}
