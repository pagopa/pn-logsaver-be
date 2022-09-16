package it.pagopa.pn.logsaver.client.safestorage;


import java.net.URI;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import it.pagopa.pn.logsaver.exceptions.InternalException;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.ApiClient;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.model.ItemUpload;
import it.pagopa.pn.logsaver.springbootcfg.PnSafeStorageConfigs;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PnSafeStorageClientImpl implements PnSafeStorageClient {
  public static final String MEDIATYPE_ZIP = "application/zip";
  public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
  public static final String SAVED = "SAVED";
  public static final String PN_AAR = "PN_AAR";

  private final FileUploadApi fileUploadApi;
  private final PnSafeStorageConfigs cfg;
  private final RestTemplate restTemplate;

  public PnSafeStorageClientImpl(RestTemplate rest, PnSafeStorageConfigs cfg) {
    ApiClient newApiClient = new ApiClient(rest);
    newApiClient.setBasePath(cfg.getSafeStorageBaseUrl());

    this.fileUploadApi = new FileUploadApi(newApiClient);
    this.cfg = cfg;
    this.restTemplate = rest;
  }


  @Override
  public ItemUpload uploadFile(ItemUpload itemUpd) {
    String sha256 = FilesUtils.computeSha256(itemUpd.filePath());

    FileCreationRequest fileCreationRequest = new FileCreationRequest();;
    fileCreationRequest.setContentType(MEDIATYPE_ZIP);
    fileCreationRequest.setDocumentType(PN_LEGAL_FACTS);
    fileCreationRequest.setStatus(SAVED);
    FileCreationResponse res = fileUploadApi.createFile(this.cfg.getSafeStorageCxId(), "SHA-256",
        sha256, fileCreationRequest);

    this.uploadContent(fileCreationRequest, res, sha256, null);
    return itemUpd.uploadKey(res.getKey());
  }

  private void uploadContent(FileCreationRequest fileCreationRequest,
      FileCreationResponse fileCreationResponse, String sha256, Path resource) {

    try {

      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("Content-type", fileCreationRequest.getContentType());
      headers.add("x-amz-checksum-sha256", sha256);
      headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

      HttpEntity<Resource> req = new HttpEntity<>(new FileSystemResource(resource), headers);

      URI url = URI.create(fileCreationResponse.getUploadUrl());
      HttpMethod method =
          fileCreationResponse.getUploadMethod() == FileCreationResponse.UploadMethodEnum.POST
              ? HttpMethod.POST
              : HttpMethod.PUT;

      ResponseEntity<String> res = restTemplate.exchange(url, method, req, String.class);

      if (res.getStatusCodeValue() != org.springframework.http.HttpStatus.OK.value()) {
        throw new InternalException("File upload failed");
      }
    } catch (InternalException ee) {
      log.error("uploadContent PnInternalException uploading file", ee);
      throw ee;
    } catch (Exception ee) {
      log.error("uploadContent Exception uploading file", ee);
      throw new InternalException("Exception uploading file", ee);
    }
  }


}
