package it.pagopa.pn.logsaver.exceptions;

public class FileSystemException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public FileSystemException(String message, Throwable cause) {
    super(message, cause);
  }

}
