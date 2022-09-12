package it.pagopa.pn.logsaver.exceptions;

public class PnInternalException extends RuntimeException {

  public PnInternalException(String message) {
    super(message);
  }

  public PnInternalException(String message, Throwable cause) {
    super(message, cause);
  }
}
