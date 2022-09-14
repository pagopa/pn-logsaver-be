package it.pagopa.pn.logsaver.model;

import lombok.Getter;

@Getter
public enum ItemType {
  cdc("cdc/", "%s/%s/%s"), logs("logs/ecs/", "%s/ecs/%s/%s");

  private String subFolfer;

  private String dailyFolferPattern2;


  private ItemType(String subFolfer, String dailyFolferPattern) {
    this.subFolfer = subFolfer;
    this.dailyFolferPattern2 = dailyFolferPattern;
  }
}
