package it.pagopa.pn.logsaver.exceptions;

public class ExternalException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ExternalException(String message) {
    super(message);
  }

  public ExternalException(String message, Throwable cause) {
    super(message, cause);
  }
}
