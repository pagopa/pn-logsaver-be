package it.pagopa.pn.logsaver.client.safestorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import it.pagopa.pn.logsaver.TestCostant;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationResponse.UploadMethodEnum;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.logsaver.model.AuditDownloadReference;
import it.pagopa.pn.logsaver.model.AuditStorage;
import it.pagopa.pn.logsaver.model.enums.AuditStorageStatus;
import it.pagopa.pn.logsaver.model.enums.ExportType;
import it.pagopa.pn.logsaver.model.enums.Retention;
import it.pagopa.pn.logsaver.springbootcfg.PnSafeStorageConfigs;

@ExtendWith(MockitoExtension.class)
class PnSafeStorageClientImplTest {

  @Mock
  private FileUploadApi fileUploadApi;

  private PnSafeStorageConfigs cfg;

  @Mock
  private RestTemplate restTemplate;

  private PnSafeStorageClient client;

  @Captor
  private ArgumentCaptor<HttpEntity<Resource>> httpEntity;

  @Captor
  private ArgumentCaptor<RequestEntity<FileCreationRequest>> httpEntityPre;


  @BeforeEach
  public void createService() {
    cfg = new PnSafeStorageConfigs();
    cfg.setSafeStorageBaseUrl("http://localhost");
    cfg.setSafeStorageDocTypesPdf(Map.of("10y", "PN_LOGS_PDF_AUDIT10Y", "5y", "PN_LOGS_PDF_AUDIT5Y",
        "120d", "PN_LOGS_PDF_TEMP"));
    cfg.setSafeStorageDocTypesZip(Map.of("10y", "PN_LOGS_ARCHIVE_AUDIT10Y", "5y",
        "PN_LOGS_ARCHIVE_AUDIT5Y", "120d", "PN_LOGS_ARCHIVE_TEMP"));
    cfg.setSafeStorageCxId("1234");
    Method init = ReflectionUtils.findMethod(PnSafeStorageConfigs.class, "initConf");
    ReflectionUtils.makeAccessible(init);
    ReflectionUtils.invokeMethod(init, cfg);
    lenient().when(restTemplate.getUriTemplateHandler())
            .thenReturn(new DefaultUriBuilderFactory());
    this.client = new PnSafeStorageClientImpl(restTemplate, cfg);
  }

  @Test
  void uploadFile() throws IOException {
    File file = new File("/tmp/test.pdf");
    FileUtils.writeStringToFile(file, "test", Charset.defaultCharset());

    FileCreationResponse respCF = new FileCreationResponse();
    respCF.setKey("KEY");
    respCF.setSecret("SECRET");
    respCF.setUploadMethod(UploadMethodEnum.PUT);
    respCF.setUploadUrl("http://s3-bucket/upload");

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(respCF));

    when(restTemplate.exchange(any(URI.class), any(HttpMethod.class),
            httpEntity.capture(), any(Class.class)))
            .thenReturn(ResponseEntity.ok(""));

    AuditStorage req =
        AuditStorage.builder().exportType(ExportType.PDF_SIGNED).filePath(List.of(file.toPath()))
            .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).build();

    AuditStorage res = client.uploadFiles(req);

    assertEquals("application/pdf", httpEntityPre.getValue().getBody().getContentType());
    assertEquals("PN_LOGS_PDF_AUDIT10Y", httpEntityPre.getValue().getBody().getDocumentType());
    assertEquals("SAVED", httpEntityPre.getValue().getBody().getStatus());

    String hash = httpEntityPre.getValue().getHeaders().get("x-checksum-value").get(0);

    assertEquals(hash, httpEntity.getValue().getHeaders().get("x-amz-checksum-sha256").get(0));
    assertEquals("SECRET", httpEntity.getValue().getHeaders().get("x-amz-meta-secret").get(0));

    verify(restTemplate, times(1)).exchange(any(URI.class), any(HttpMethod.class),
        any(HttpEntity.class), any(Class.class));
    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertNull(res.error());
    assertTrue(res.uploadKey().values().contains("KEY"));

    Files.delete(file.toPath());
  }


  @Test
  void uploadFile_CreateFileInternalServerError() throws IOException {
    File file = new File("/tmp/test.pdf");

    FileUtils.writeStringToFile(file, "test", Charset.defaultCharset());

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.internalServerError().body(""));

    AuditStorage req =
        AuditStorage.builder().exportType(ExportType.PDF_SIGNED).filePath(List.of(file.toPath()))
            .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).build();

    AuditStorage res = client.uploadFiles(req);

    assertEquals("application/pdf", httpEntityPre.getValue().getBody().getContentType());
    assertEquals("PN_LOGS_PDF_AUDIT10Y", httpEntityPre.getValue().getBody().getDocumentType());
    assertEquals("SAVED", httpEntityPre.getValue().getBody().getStatus());

    verify(restTemplate, times(1)).exchange(
            any(RequestEntity.class), any(ParameterizedTypeReference.class));

    verify(restTemplate, never()).exchange(
            any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));

    assertNotNull(res);
    assertNotNull(res.error());

    Files.delete(file.toPath());
  }



  @Test
  void uploadFile_UploadContentInternalServerError() throws IOException {
    File file = new File("/tmp/test.pdf");

    FileUtils.writeStringToFile(file, "test", Charset.defaultCharset());

    FileCreationResponse respCF = new FileCreationResponse();
    respCF.setKey("KEY");
    respCF.setSecret("SECRET");
    respCF.setUploadMethod(UploadMethodEnum.POST);
    respCF.setUploadUrl("http://s3-bucket/upload");

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(respCF));

    when(restTemplate.exchange(any(URI.class), any(HttpMethod.class),
            httpEntity.capture(), any(Class.class)))
            .thenReturn(ResponseEntity.internalServerError().body(""));

    AuditStorage req =
        AuditStorage.builder().exportType(ExportType.PDF_SIGNED).filePath(List.of(file.toPath()))
            .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).build();

    AuditStorage res = client.uploadFiles(req);

    assertEquals("application/pdf", httpEntityPre.getValue().getBody().getContentType());
    assertEquals("PN_LOGS_PDF_AUDIT10Y", httpEntityPre.getValue().getBody().getDocumentType());
    assertEquals("SAVED", httpEntityPre.getValue().getBody().getStatus());

    String hash = httpEntityPre.getValue().getHeaders().get("x-checksum-value").get(0);
    assertEquals(hash, httpEntity.getValue().getHeaders().get("x-amz-checksum-sha256").get(0));
    assertEquals("SECRET", httpEntity.getValue().getHeaders().get("x-amz-meta-secret").get(0));

    verify(restTemplate, times(1)).exchange(any(URI.class), any(HttpMethod.class),
        any(HttpEntity.class), any(Class.class));
    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertNotNull(res.error());

    Files.delete(file.toPath());
  }


  @Test
  void uploadFile_Exception() throws IOException {
    File file = new File("/tmp/test.pdf");

    FileUtils.writeStringToFile(file, "test", Charset.defaultCharset());

    FileCreationResponse respCF = new FileCreationResponse();
    respCF.setKey("KEY");
    respCF.setSecret("SECRET");
    respCF.setUploadMethod(UploadMethodEnum.POST);
    respCF.setUploadUrl("http://s3-bucket/upload");

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(respCF));

    when(restTemplate.exchange(any(URI.class), any(HttpMethod.class),
            httpEntity.capture(), any(Class.class)))
            .thenThrow(new RuntimeException("Connection timeout"));

    AuditStorage req =
        AuditStorage.builder().exportType(ExportType.PDF_SIGNED).filePath(List.of(file.toPath()))
            .logDate(TestCostant.LOGDATE).retention(Retention.AUDIT10Y).build();

    AuditStorage res = client.uploadFiles(req);

    assertEquals("application/pdf", httpEntityPre.getValue().getBody().getContentType());
    assertEquals("PN_LOGS_PDF_AUDIT10Y", httpEntityPre.getValue().getBody().getDocumentType());
    assertEquals("SAVED", httpEntityPre.getValue().getBody().getStatus());

    String hash = httpEntityPre.getValue().getHeaders().get("x-checksum-value").get(0);

    assertEquals(hash, httpEntity.getValue().getHeaders().get("x-amz-checksum-sha256").get(0));
    assertEquals("SECRET", httpEntity.getValue().getHeaders().get("x-amz-meta-secret").get(0));

    verify(restTemplate, times(1)).exchange(any(URI.class), any(HttpMethod.class),
        any(HttpEntity.class), any(Class.class));
    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertNotNull(res.error());

    Files.delete(file.toPath());
  }



  @Test
  void downloadFileInfo() {
    FileDownloadResponse respCF = buildFileDownloadResponse("http://download-url/file.pdf");

    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(respCF));


    AuditDownloadReference req = AuditDownloadReference.builder().logDate(TestCostant.LOGDATE)
        .status(AuditStorageStatus.SENT).uploadKey("updKey").build();

    AuditDownloadReference res = client.downloadFileInfo(req);

    verify(restTemplate, times(1)).exchange(any(RequestEntity.class),
        any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertNull(res.error());
    assertEquals("http://download-url/file.pdf", res.downloadUrl());
  }

  @Test
  void downloadFileInfo_NotFound() {
    when(restTemplate.exchange(httpEntityPre.capture(), any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.notFound().build());


    AuditDownloadReference req = AuditDownloadReference.builder().logDate(TestCostant.LOGDATE)
        .status(AuditStorageStatus.SENT).uploadKey("updKey").build();

    AuditDownloadReference res = client.downloadFileInfo(req);

    verify(restTemplate, times(1)).exchange(
            any(RequestEntity.class), any(ParameterizedTypeReference.class));

    assertNotNull(res);
    assertNotNull(res.error());
    assertEquals(RestClientException.class, res.error().getClass());
  }


  @Test
  void downloadFile() {
    AuditDownloadReference expected = AuditDownloadReference.builder()
            .logDate(TestCostant.LOGDATE)
            .status(AuditStorageStatus.SENT)
            .uploadKey("updKey")
            .build();

    when(restTemplate.execute(any(URI.class), any(HttpMethod.class), any(), any()))
            .thenReturn(expected);

    AuditDownloadReference req = AuditDownloadReference.builder()
            .logDate(TestCostant.LOGDATE)
            .status(AuditStorageStatus.SENT)
            .downloadUrl("https://test.it/file.pdf")
            .uploadKey("updKey")
            .build();

    AuditDownloadReference res = client.downloadFile(req, UnaryOperator.identity());

    verify(restTemplate, times(1))
            .execute(any(URI.class), any(HttpMethod.class), any(), any());

    assertNotNull(res);
    assertNull(res.error());
  }

  @Test
  void downloadFile_NetworkError() {
    when(restTemplate.execute(any(URI.class), any(HttpMethod.class), any(), any()))
            .thenThrow(new RestClientException("Connection refused"));

    AuditDownloadReference req = AuditDownloadReference.builder().logDate(TestCostant.LOGDATE)
        .status(AuditStorageStatus.SENT).downloadUrl("https://test.it/").uploadKey("updKey")
        .build();

    AuditDownloadReference res = client.downloadFile(req, UnaryOperator.identity());

    verify(restTemplate, times(1))
            .execute(any(URI.class), any(HttpMethod.class), any(), any());

    assertNotNull(res);
    assertNotNull(res.error());
    assertEquals(RestClientException.class, res.error().getClass());
  }


  @Test
  void downloadFile_NullUrlError() {
    // Nessun mock su execute(): il metodo non viene mai raggiunto
    AuditDownloadReference req = AuditDownloadReference.builder()
            .logDate(TestCostant.LOGDATE)
            .status(AuditStorageStatus.SENT)
            .uploadKey("updKey")
            // downloadUrl NON impostato → null → NullPointerException in URI.create()
            .build();

    AuditDownloadReference res = client.downloadFile(req, UnaryOperator.identity());

    verify(restTemplate, never())
            .execute(any(URI.class), any(HttpMethod.class), any(), any());

    assertNotNull(res);
    assertNotNull(res.error());
  }


  private FileDownloadResponse buildFileDownloadResponse(String downloadUrl) {
    FileDownloadResponse resp = new FileDownloadResponse();
    resp.setKey("updKey");
    resp.setContentType("application/pdf");
    resp.setDocumentStatus("SAVED");
    resp.setDocumentType("PN_LOGS_PDF_AUDIT10Y");
    FileDownloadInfo info = new FileDownloadInfo();
    info.setUrl(downloadUrl);
    resp.setDownload(info);
    return resp;
  }
}