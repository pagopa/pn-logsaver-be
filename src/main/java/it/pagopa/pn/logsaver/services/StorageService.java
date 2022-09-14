package it.pagopa.pn.logsaver.services;

import java.util.List;
import it.pagopa.pn.logsaver.model.ArchiveInfo;

public interface StorageService {

  void send(List<ArchiveInfo> files);

}
