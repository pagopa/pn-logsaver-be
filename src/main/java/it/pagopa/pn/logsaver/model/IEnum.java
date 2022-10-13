package it.pagopa.pn.logsaver.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.EnumUtils;
import lombok.experimental.UtilityClass;


@UtilityClass
public class IEnum {
  public static <T extends Enum<T>> List<String> valuesAsString(Class<T> enumType) {
    return EnumUtils.getEnumList(enumType).stream().map(Enum::name).collect(Collectors.toList());
  }

  public static <T extends Enum<T>> List<String> valuesAsString(Collection<T> list) {
    return list.stream().map(Enum::name).collect(Collectors.toList());
  }

  public static <T extends Enum<T>> Set<T> values(List<String> list, Class<T> enumType) {
    return list.stream().map(v -> Enum.valueOf(enumType, v)).collect(Collectors.toSet());
  }
}
