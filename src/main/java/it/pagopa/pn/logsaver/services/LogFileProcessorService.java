package it.pagopa.pn.logsaver.services;

import java.util.List;
import java.util.stream.Stream;
import it.pagopa.pn.logsaver.model.DailyContextCfg;
import it.pagopa.pn.logsaver.model.LogFileReference;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator;
import it.pagopa.pn.logsaver.utils.StreamingExportCoordinator.UploadedPart;


public interface LogFileProcessorService {

  List<UploadedPart> process(Stream<LogFileReference> fileStream, DailyContextCfg dailyCtx,
      StreamingExportCoordinator coordinator);

}
