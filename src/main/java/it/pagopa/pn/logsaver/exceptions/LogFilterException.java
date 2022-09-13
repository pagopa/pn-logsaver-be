package it.pagopa.pn.logsaver.exceptions;

public class LogFilterException extends RuntimeException {

  public LogFilterException(String message) {
    super(message);
  }

  public LogFilterException(String message, Throwable cause) {
    super(message, cause);
  }

}
