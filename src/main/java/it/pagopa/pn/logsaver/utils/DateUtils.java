package it.pagopa.pn.logsaver.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateUtils {

  public static LocalDate parse(String date) {
    return StringUtils.isEmpty(date) ? null : LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
  }

  public static String format(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
    return date.format(formatter);
  }

  /*
   * TODO Rimuovere la data cablata
   */
  public static LocalDate yesterday() {
    return parse("2022-07-12");
    // return LocalDate.now(italianZoneId).minusDays(1);
  }

  public static List<LocalDate> getDatesRange(LocalDate startDate, LocalDate endDate) {
    return startDate.plusDays(1).datesUntil(endDate).collect(Collectors.toList());
  }
}
