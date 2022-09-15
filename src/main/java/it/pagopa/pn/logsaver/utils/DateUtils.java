package it.pagopa.pn.logsaver.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

  private static final ZoneId italianZoneId = ZoneId.of("Europe/Rome");

  private DateUtils() {}


  public static String formatDate(Instant instant) {
    if (instant == null)
      return null;

    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
    return LocalDate.ofInstant(instant, italianZoneId).format(formatter);
  }

  public static String formatTime(ZonedDateTime datetime) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    return datetime.format(formatter.withZone(italianZoneId));
  }
  /*
   * public static ZonedDateTime parseDate(String date) { DateTimeFormatter formatter =
   * DateTimeFormatter.ISO_DATE; LocalDate locdate = LocalDate.parse(date, formatter);
   * 
   * return locdate.atStartOfDay(italianZoneId); }
   */

  public static ZonedDateTime atStartOfDay(Instant instant) {
    LocalDate locdate = LocalDate.ofInstant(instant, italianZoneId);
    return locdate.atStartOfDay(italianZoneId);
  }

  public static ZonedDateTime parseTime(String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    return formatter.parse(date, ZonedDateTime::from);
  }


  public static LocalDate parse(String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
    return LocalDate.parse(date, formatter);
  }

  public static String format(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
    return date.format(formatter);
  }


  public static LocalDate yesterday() {
    return LocalDate.now(italianZoneId).minusDays(1);
  }

}
