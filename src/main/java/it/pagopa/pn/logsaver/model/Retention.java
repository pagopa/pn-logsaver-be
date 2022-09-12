package it.pagopa.pn.logsaver.model;

import lombok.Getter;

@Getter
public enum Retention {

  AUDIT10Y("10y", 10), AUDIT5Y("5y", 5), AUDIT2Y("2y", 2);

  private String folder;

  private int retentionYears;

  Retention(String folder, int retentionYears) {
    this.folder = folder;
    this.retentionYears = retentionYears;
  }
}
