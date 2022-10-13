package it.pagopa.pn.logsaver.services;

import java.util.List;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.model.AuditFile;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;


public interface LogFileProcessorService {

  List<AuditFile> process(Stream<LogFileReference> fileStream, DailyContextCfg dailyCtx);

}
