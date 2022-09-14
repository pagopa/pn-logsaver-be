package it.pagopa.pn.logsaver.client.safestorage;

import it.pagopa.pn.logsaver.model.ItemUpload;

public interface PnSafeStorageClient {

  ItemUpload uploadFile(ItemUpload itemUpd);

}
