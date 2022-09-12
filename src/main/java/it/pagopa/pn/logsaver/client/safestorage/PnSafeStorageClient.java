package it.pagopa.pn.logsaver.client.safestorage;

import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.logsaver.model.FileCreationWithContentRequest;

public interface PnSafeStorageClient {

  FileCreationResponse createFile(FileCreationRequest fileCreationRequest, String sha256);

  void uploadContent(FileCreationWithContentRequest fileCreationRequest,
      FileCreationResponse fileCreationResponse, String sha256);

  OperationResultCodeResponse updateFileMetadata(UpdateFileMetadataRequest fileUpdMetadataRequest,
      FileCreationResponse fileCreationResponse);

}
