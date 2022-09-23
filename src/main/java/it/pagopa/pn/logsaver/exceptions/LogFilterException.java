package it.pagopa.pn.logsaver.exceptions;

public class LogFilterException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public LogFilterException(String message) {
    super(message);
  }

  public LogFilterException(String message, Throwable cause) {
    super(message, cause);
  }

}
