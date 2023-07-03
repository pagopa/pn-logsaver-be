package it.pagopa.pn.logsaver.model.enums;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum Retention {

  AUDIT10Y("10y", "'audit-log-10y-'yyyy-MM-dd'_part%d'", "10 anni"), AUDIT5Y("5y",
      "'audit-log-5y-'yyyy-MM-dd'_part%d'",
      "5 anni"), DEVELOPER("120d", "'developers-log-'yyyy-MM-dd'_part%d'", "120 giorni");

  private String code;

  private String fileNamePattern;

  private String text;


  Retention(String code, String nameFormat, String text) {
    this.code = code;
    this.fileNamePattern = nameFormat;
    this.text = text;
  }


  public static List<String> valuesAsString() {
    return IEnum.valuesAsString(Retention.class);
  }

  public static List<String> valuesAsString(Collection<Retention> list) {
    return IEnum.valuesAsString(list);
  }

  public static Set<Retention> values(List<String> list) {
    return IEnum.values(list, Retention.class);
  }

  public static Retention valueFromCode(String code) {
    return Stream.of(Retention.values()).filter(r -> r.getCode().equalsIgnoreCase(code)).findFirst()
        .orElseThrow();
  }
}
