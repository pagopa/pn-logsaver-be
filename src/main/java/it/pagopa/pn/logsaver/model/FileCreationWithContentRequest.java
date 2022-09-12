package it.pagopa.pn.logsaver.model;

import it.pagopa.pn.logsaver.generated.openapi.clients.safestorage.model.FileCreationRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileCreationWithContentRequest extends FileCreationRequest {
  private byte[] content;
}
