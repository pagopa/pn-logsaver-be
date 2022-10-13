package it.pagopa.pn.logsaver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DateUtilsTest {


  @Test
  void parse_WhenEmpty_TherReturnNUll() {
    LocalDate d = DateUtils.parse("");
    assertNull(d);
  }



  @Test
  void getDatesRange() {
    List<LocalDate> list =
        DateUtils.getDatesRange(LocalDate.parse("2022-07-08"), LocalDate.parse("2022-07-10"));
    assertEquals(1, list.size());
    assertTrue(list.contains(LocalDate.parse("2022-07-09")));
  }
}
