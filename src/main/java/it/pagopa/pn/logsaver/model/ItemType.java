package it.pagopa.pn.logsaver.model;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.EnumUtils;
import lombok.Getter;

@Getter
public enum ItemType {
  cdc("cdc/", "%s/%s/%s"), logs("logs/ecs/", "%s/ecs/%s/%s");

  private String subFolfer;

  private String dailyFolferPattern2;


  private ItemType(String subFolfer, String dailyFolferPattern) {
    this.subFolfer = subFolfer;
    this.dailyFolferPattern2 = dailyFolferPattern;
  }

  public static List<String> valuesAsString() {
    return EnumUtils.getEnumList(ItemType.class).stream().map(ItemType::name)
        .collect(Collectors.toList());
  }

  public static List<String> valuesAsString(List<ItemType> list) {
    return list.stream().map(ItemType::name).collect(Collectors.toList());
  }

  public static List<ItemType> values(List<String> list) {
    return list.stream().map(ItemType::valueOf).collect(Collectors.toList());
  }
}
