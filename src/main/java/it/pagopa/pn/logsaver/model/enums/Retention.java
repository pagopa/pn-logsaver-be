package it.pagopa.pn.logsaver.model.enums;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum Retention {

  AUDIT10Y("10y", "'audit-log-10y-'yyyy-MM-dd'_part%d'", "10 anni", Duration.ofDays(365 * 10)),
  AUDIT5Y("5y",   "'audit-log-5y-'yyyy-MM-dd'_part%d'","5 anni", Duration.ofDays(365 * 5)),
  AUDIT2Y("2y",      "'audit-log-2y-'yyyy-MM-dd'_part%d'","2 anni", Duration.ofDays(365 * 2)),
  DEVELOPER("120d", "'developers-log-'yyyy-MM-dd'_part%d'", "120 giorni", Duration.ofDays(120));

  private String code;

  private String fileNamePattern;

  private String text;

  private Duration duration;


  Retention(String code, String nameFormat, String text, Duration duration) {
    this.code = code;
    this.fileNamePattern = nameFormat;
    this.text = text;
    this.duration = duration;
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
