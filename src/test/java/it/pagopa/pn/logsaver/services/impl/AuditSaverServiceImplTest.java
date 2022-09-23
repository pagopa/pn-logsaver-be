package it.pagopa.pn.logsaver.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import it.pagopa.pn.logsaver.services.AuditSaverService;
import it.pagopa.pn.logsaver.services.ItemProcessorService;
import it.pagopa.pn.logsaver.services.ItemReaderService;
import it.pagopa.pn.logsaver.services.StorageService;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;

public class AuditSaverServiceImplTest {

  @Mock
  private ItemReaderService readerService;
  @Mock
  private ItemProcessorService procService;
  @Mock
  private StorageService storageService;
  @Mock
  private LogSaverCfg cfg;

  private AuditSaverService service;

  @BeforeEach
  void setUp() {
    service = new AuditSaverServiceImpl(readerService, procService, storageService, cfg);
  }
}
