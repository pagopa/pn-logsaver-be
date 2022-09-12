package it.pagopa.pn.logsaver.client.safestorage;


import java.net.URI;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import it.pagopa.pn.logsaver.exceptions.PnInternalException;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.ApiClient;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileMetadataUpdateApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.logsaver.model.FileCreationWithContentRequest;
import it.pagopa.pn.logsaver.springbootcfg.PnSafeStorageConfigs;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PnSafeStorageClientImpl implements PnSafeStorageClient {

  private final FileUploadApi fileUploadApi;
  private final FileMetadataUpdateApi fileUpdMetadataApi;
  private final PnSafeStorageConfigs cfg;
  private final RestTemplate restTemplate;

  public PnSafeStorageClientImpl(RestTemplate rest, PnSafeStorageConfigs cfg) {
    ApiClient newApiClient = new ApiClient(rest);
    newApiClient.setBasePath(cfg.getSafeStorageBaseUrl());

    this.fileUploadApi = new FileUploadApi(newApiClient);
    this.fileUpdMetadataApi = new FileMetadataUpdateApi(newApiClient);
    this.cfg = cfg;
    this.restTemplate = rest;
  }


  @Override
  public FileCreationResponse createFile(FileCreationRequest fileCreationRequest, String sha256) {
    return fileUploadApi.createFile(this.cfg.getSafeStorageCxId(), "SHA-256", sha256,
        fileCreationRequest);
  }

  @Override
  public void uploadContent(FileCreationWithContentRequest fileCreationRequest,
      FileCreationResponse fileCreationResponse, String sha256) {

    try {

      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("Content-type", fileCreationRequest.getContentType());
      headers.add("x-amz-checksum-sha256", sha256);
      headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

      HttpEntity<Resource> req =
          new HttpEntity<>(new ByteArrayResource(fileCreationRequest.getContent()), headers);

      URI url = URI.create(fileCreationResponse.getUploadUrl());
      HttpMethod method =
          fileCreationResponse.getUploadMethod() == FileCreationResponse.UploadMethodEnum.POST
              ? HttpMethod.POST
              : HttpMethod.PUT;

      ResponseEntity<String> res = restTemplate.exchange(url, method, req, String.class);

      if (res.getStatusCodeValue() != org.springframework.http.HttpStatus.OK.value()) {
        throw new PnInternalException("File upload failed");
      }
    } catch (PnInternalException ee) {
      log.error("uploadContent PnInternalException uploading file", ee);
      throw ee;
    } catch (Exception ee) {
      log.error("uploadContent Exception uploading file", ee);
      throw new PnInternalException("Exception uploading file", ee);
    }
  }

  @Override
  public OperationResultCodeResponse updateFileMetadata(
      UpdateFileMetadataRequest fileCreationRequest, FileCreationResponse fileCreationResponse) {
    return fileUpdMetadataApi.updateFileMetadata(fileCreationResponse.getSecret(),
        this.cfg.getSafeStorageCxId(), fileCreationRequest);
  }
}
