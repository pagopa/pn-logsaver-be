package it.pagopa.pn.logsaver.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.List;
import org.springframework.stereotype.Service;
import it.pagopa.pn.logsaver.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.logsaver.model.ArchiveInfo;
import it.pagopa.pn.logsaver.model.FileCreationResponseInt;
import it.pagopa.pn.logsaver.model.FileCreationWithContentRequest;
import it.pagopa.pn.logsaver.model.Retention;
import it.pagopa.pn.logsaver.utils.FilesUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SafeStorageServiceImpl implements SafeStorageService {
  public static final String MEDIATYPE_ZIP = "application/zip";
  public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
  public static final String SAVED = "SAVED";
  public static final String PN_AAR = "PN_AAR";

  private final PnSafeStorageClient safeStorageClient;

  public SafeStorageServiceImpl(PnSafeStorageClient safeStorageClient) {
    this.safeStorageClient = safeStorageClient;
  }

  @Override
  public void send(List<ArchiveInfo> files) {

    files.stream().forEach(this::send);

  }

  private void send(ArchiveInfo file) {

    OffsetDateTime retentionUntil = computeRetention(file.getLogDate(), file.getRetention());
    FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
    fileCreationRequest.setContent(FilesUtils.fileToByteArray(file.getFilePath()));
    fileCreationRequest.setContentType(MEDIATYPE_ZIP);
    fileCreationRequest.setDocumentType(PN_LEGAL_FACTS);
    fileCreationRequest.setStatus(SAVED);

    createAndUploadContent(fileCreationRequest, retentionUntil);
  }

  private FileCreationResponseInt createAndUploadContent(
      FileCreationWithContentRequest fileCreationRequest, OffsetDateTime retentionUntil) {

    log.debug("Start call createAndUploadFile - documentType={} filesize={}",
        fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

    String sha256 = FilesUtils.computeSha256(fileCreationRequest.getContent());

    FileCreationResponse fileCreationResponse =
        safeStorageClient.createFile(fileCreationRequest, sha256);

    log.info("File sended in safestorage {}", fileCreationResponse);

    safeStorageClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256);

    // TODO check
    UpdateFileMetadataRequest fileUpdMetadataRequest = new UpdateFileMetadataRequest();
    fileUpdMetadataRequest.setRetentionUntil(null);
    safeStorageClient.updateFileMetadata(fileUpdMetadataRequest, fileCreationResponse);

    FileCreationResponseInt fileCreationResponseInt =
        FileCreationResponseInt.builder().key(fileCreationResponse.getKey()).build();

    log.info("createAndUploadContent file uploaded successfully key={} sha256={}",
        fileCreationResponseInt.getKey(), sha256);

    return fileCreationResponseInt;
  }

  private OffsetDateTime computeRetention(LocalDate logDate, Retention retention) {
    return logDate.plusYears(retention.getRetentionYears()).atTime(OffsetTime.now());
  }


}
