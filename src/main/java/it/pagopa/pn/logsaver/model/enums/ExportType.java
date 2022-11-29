package it.pagopa.pn.logsaver.model.enums;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.Getter;


public enum ExportType {
  ZIP(".zip", "application/zip", ExportType.ZIP_S), PDF_SIGNED(".pdf", "application/pdf",
      ExportType.PDF_S);

  public static final String ZIP_S = "ZIP";
  public static final String PDF_S = "PDF_SIGNED";
  @Getter
  private String extension;
  @Getter
  private String name;
  @Getter
  private String mediaType;

  ExportType(String extension, String mediaType, String name) {
    this.extension = extension;
    this.mediaType = mediaType;
    this.name = name;
  }

  public static List<String> valuesAsString() {
    return IEnum.valuesAsString(ExportType.class);
  }

  public static List<String> valuesAsString(Collection<ExportType> list) {
    return IEnum.valuesAsString(list);
  }

  public static Set<ExportType> values(List<String> list) {
    return IEnum.values(list, ExportType.class);
  }
}
