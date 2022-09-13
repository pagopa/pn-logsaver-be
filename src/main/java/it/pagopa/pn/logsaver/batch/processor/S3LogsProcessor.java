package it.pagopa.pn.logsaver.batch.processor;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.services.ItemProcessorServiceImpl;

@Component
@StepScope
public class S3LogsProcessor implements ItemProcessor<Item, Item> {

  private StepExecution stepExecution;
  private final ItemProcessorServiceImpl service;

  public S3LogsProcessor(ItemProcessorServiceImpl service) {
    this.service = service;
  }


  @Override
  public Item process(Item item) throws Exception {
    // service.temporaryStore(item);
    Boolean readerExahausted = stepExecution.getExecutionContext().containsKey("readerExhausted");
    if (readerExahausted) {
      stepExecution.getExecutionContext().put("processorExhausted", Boolean.TRUE);
    }
    return item;
  }

  @BeforeStep
  public void setStepExecution(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

}
