package it.pagopa.pn.logsaver.exceptions;

import java.io.IOException;

public class UncheckedIOException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UncheckedIOException(String message) {
    super(message);
  }

  public UncheckedIOException(String message, IOException cause) {
    super(message, cause);
  }

}
