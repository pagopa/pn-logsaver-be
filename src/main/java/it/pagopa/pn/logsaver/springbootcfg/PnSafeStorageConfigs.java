package it.pagopa.pn.logsaver.springbootcfg;

import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.google.common.collect.HashBasedTable;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "pn")
@Getter
@Setter
public class PnSafeStorageConfigs {

  private String safeStorageBaseUrl;

  private String safeStorageCxId;
  private Map<String, String> safeStorageDocTypesPdf;
  private Map<String, String> safeStorageDocTypesZip;
  private HashBasedTable<Retention, ExportType, String> docTypeTable = HashBasedTable.create(0, 0);

  @PostConstruct
  void initConf() {
    populateConfigurationTable(safeStorageDocTypesPdf, ExportType.PDF_SIGNED);
    populateConfigurationTable(safeStorageDocTypesZip, ExportType.ZIP);
  }

  private void populateConfigurationTable(Map<String, String> safeStorageDocTypes,
      ExportType expType) {
    safeStorageDocTypes.entrySet().forEach(entry -> docTypeTable
        .put(Retention.valueFromCode(entry.getKey()), expType, entry.getValue()));
  }

  public String getStorageDocumentType(AuditStorage audit) {
    return docTypeTable.get(audit.retention(), audit.exportType());
  }
}
