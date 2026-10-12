package it.pagopa.pn.logsaver.dao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ContinuosExecutionEntityTest {

  @Test
  void constructor_anyLatestExecutionDate_sortKeyIsFixed() {
    ContinuosExecutionEntity first = new ContinuosExecutionEntity(LocalDate.parse("2023-03-01"));
    ContinuosExecutionEntity other = new ContinuosExecutionEntity(LocalDate.parse("2025-11-20"));

    assertEquals("2023-03-01", first.getLogDate());
    assertEquals("2023-03-01", other.getLogDate());
    assertEquals(LocalDate.parse("2025-11-20"), other.getLatestExecutionDate());
  }
}
