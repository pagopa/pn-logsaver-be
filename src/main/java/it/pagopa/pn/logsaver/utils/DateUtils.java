package it.pagopa.pn.logsaver.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateUtils {

  private static final ZoneId italianZoneId = ZoneId.of("Europe/Rome");

  public static LocalDate parse(String date) {
    return StringUtils.isEmpty(date) ? null : LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
  }

  public static String format(LocalDate date) {
    return date.format(DateTimeFormatter.ISO_DATE);
  }

  public static LocalDate yesterday() {
    return LocalDate.now(italianZoneId).minusDays(1);
  }

  public static List<LocalDate> getDatesRange(LocalDate startDate, LocalDate endDate) {
    return startDate.plusDays(1).datesUntil(endDate).collect(Collectors.toList());
  }

  public static String isoDateTime() {
    return LocalDateTime.now(italianZoneId).format(DateTimeFormatter.ISO_DATE_TIME);
  }

  public static String dateTime() {
    return LocalDateTime.now(italianZoneId)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
  }

  public static String getYear(LocalDate data) {
    return data.format(DateTimeFormatter.ofPattern("yyyy"));
  }
}
