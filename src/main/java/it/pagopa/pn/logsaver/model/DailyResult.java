package it.pagopa.pn.logsaver.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@SuperBuilder
public abstract class DailyResult<T extends ErrorAware> {

  abstract List<T> getItems();

  abstract boolean auditFileHasError(T err);

  abstract String detailErrorMessage(T err);

  abstract String detailSuccessMessage(T err);

  abstract String handleBaseMessage(T err);

  private Throwable error;


  public boolean hasErrors() {
    return Objects.nonNull(error) || CollectionUtils.emptyIfNull(getItems()).stream()
        .filter(ErrorAware::hasError).count() > 0;
  }

  public List<String> successMessages() {
    return messages(au -> !this.auditFileHasError(au), this::handleSuccessMessage);
  }

  public List<String> errorMessages() {
    return messages(this::auditFileHasError, this::handleErrorMessage);
  }

  private String handleErrorMessage(T item) {
    return handleBaseMessage(item).concat(detailErrorMessage(item));
  }

  private String handleSuccessMessage(T item) {
    return handleBaseMessage(item).concat(detailSuccessMessage(item));
  }

  private List<String> messages(Predicate<T> predicate, Function<T, String> mapper) {
    return CollectionUtils.emptyIfNull(getItems()).stream().filter(predicate).map(mapper)
        .collect(Collectors.toList());
  }

}
