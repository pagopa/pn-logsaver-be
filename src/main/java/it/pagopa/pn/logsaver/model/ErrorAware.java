package it.pagopa.pn.logsaver.model;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public interface ErrorAware {

  Throwable error();


  default boolean hasError() {
    return Objects.nonNull(error());
  }

  default String getErrorMessage() {
    return hasError() ? error().getMessage() : StringUtils.EMPTY;
  }
}
