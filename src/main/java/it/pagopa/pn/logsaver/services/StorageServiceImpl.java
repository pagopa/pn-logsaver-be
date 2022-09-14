package it.pagopa.pn.logsaver.services;

import java.util.List;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.logsaver.model.ArchiveInfo;
import it.pagopa.pn.logsaver.model.ItemUpload;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {
  public static final String MEDIATYPE_ZIP = "application/zip";
  public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
  public static final String SAVED = "SAVED";
  public static final String PN_AAR = "PN_AAR";

  private final PnSafeStorageClient safeStorageClient;

  public StorageServiceImpl(PnSafeStorageClient safeStorageClient) {
    this.safeStorageClient = safeStorageClient;
  }

  @Override
  public void send(List<ArchiveInfo> files) {

    files.stream().forEach(this::send);

  }

  private void send(ArchiveInfo file) {

    ItemUpload itemUpd = safeStorageClient.uploadFile(ItemUpload.from(file));
    // TODO UPD DynamoDb
  }



}
