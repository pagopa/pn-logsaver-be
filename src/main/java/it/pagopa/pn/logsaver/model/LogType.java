package it.pagopa.pn.logsaver.model;

import lombok.Getter;

@Getter
public enum LogType {
  cdc("cdc/", "%s/%s/%s"), logs("logs/ecs/", "%s/ecs/%s/%s");

  private String subFolfer;

  private String dailyFolferPattern;


  private LogType(String subFolfer, String dailyFolferPattern) {
    this.subFolfer = subFolfer;
    this.dailyFolferPattern = dailyFolferPattern;
  }
}
