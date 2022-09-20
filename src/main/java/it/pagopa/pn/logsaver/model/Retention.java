package it.pagopa.pn.logsaver.model;

import lombok.Getter;

@Getter
public enum Retention {

  AUDIT10Y("10y", "'audit-log-10y-'yyyy-MM-dd", "10 anni"), AUDIT5Y("5y",
      "'audit-log-5y-'yyyy-MM-dd",
      "5 anni"), DEVELOPER("120d", "'developers-log-'yyyy-MM-dd", "120 giorni");

  private String folder;

  private String nameFormat;

  private String text;


  Retention(String folder, String nameFormat, String text) {
    this.folder = folder;
    this.nameFormat = nameFormat;
    this.text = text;
  }
}
