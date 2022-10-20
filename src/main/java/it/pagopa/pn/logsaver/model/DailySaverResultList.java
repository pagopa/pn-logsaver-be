package it.pagopa.pn.logsaver.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@Slf4j
public class DailySaverResultList implements LogSaverResult {

  @Default
  private List<DailySaverResult> results = new ArrayList<>();

  @Override
  public int exitCodeAndLogResult() {
    long exitCode = results.stream().map(resDaily -> {
      log.info(resDaily.toString());
      resDaily.successMessages().stream().forEach(log::info);
      resDaily.errorMessages().stream().forEach(log::info);
      return resDaily;
    }).filter(DailySaverResult::hasErrors).count();
    exitCode = exitCode > 0 ? 1 : 0;
    return Math.toIntExact(exitCode);
  }


}
