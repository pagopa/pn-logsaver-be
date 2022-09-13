package it.pagopa.pn.logsaver.model;

import lombok.Getter;

@Getter
public enum Retention {

  AUDIT10Y("10y", "'audit-log-10y-'yyyy-MM-dd'.zip'"), AUDIT5Y("5y",
      "'audit-log-5y-'yyyy-MM-dd'.zip'"), GENERIC("120d", "'developers-log-y'yyy-MM-dd'.zip'");

  private String folder;

  private String zipNameFormat;


  Retention(String folder, String zipNameFormat) {
    this.folder = folder;
    this.zipNameFormat = zipNameFormat;
  }
}
