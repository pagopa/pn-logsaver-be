package it.pagopa.pn.logsaver.services;

import java.io.InputStream;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;


public interface LogFileReaderService {


  Stream<LogFileReference> findLogFiles(DailyContextCfg dailyCtx);

  InputStream getContent(String key);

}
