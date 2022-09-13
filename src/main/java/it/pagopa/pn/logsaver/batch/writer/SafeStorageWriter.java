package it.pagopa.pn.logsaver.batch.writer;

import java.util.List;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.services.ItemProcessorServiceImpl;
import it.pagopa.pn.logsaver.services.SafeStorageService;

@Component
@StepScope
public class SafeStorageWriter implements ItemWriter<Item> {


  private StepExecution stepExecution;
  private final ItemProcessorServiceImpl service;
  private final SafeStorageService storageService;

  public SafeStorageWriter(ItemProcessorServiceImpl service, SafeStorageService storageService) {
    this.service = service;
    this.storageService = storageService;
  }


  @Override
  public void write(List<? extends Item> items) throws Exception {
    Boolean processorExhausted =
        stepExecution.getExecutionContext().containsKey("processorExhausted");
    if (processorExhausted) {
      stepExecution.getExecutionContext().put("processorExhausted", Boolean.TRUE);
      // List<ArchiveInfo> archives = service.zipAllItemsByRetention();
      // storageService.send(archives);
    }
  }

  @BeforeStep
  public void setStepExecution(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

}
