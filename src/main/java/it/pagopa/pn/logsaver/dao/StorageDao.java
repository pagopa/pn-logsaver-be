package it.pagopa.pn.logsaver.dao;

import java.time.LocalDate;
import java.util.List;
import it.pagopa.pn.logsaver.dao.entity.AuditStorage;
import it.pagopa.pn.logsaver.dao.entity.LatestSuccessStorage;
import it.pagopa.pn.logsaver.model.Retention;


public interface StorageDao {

  List<AuditStorage> getItems(LocalDate dateFrom, LocalDate dateTo, List<Retention> retentions);

  LatestSuccessStorage latestSuccess();

  LatestSuccessStorage updateLatestSuccess(LocalDate latestSuccessDay, List<String> types);

}
