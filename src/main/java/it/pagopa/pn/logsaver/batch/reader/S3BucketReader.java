package it.pagopa.pn.logsaver.batch.reader;

import java.util.ArrayList;
import java.util.Iterator;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import it.pagopa.pn.logsaver.client.s3.S3BucketClient;
import it.pagopa.pn.logsaver.model.Item;
import it.pagopa.pn.logsaver.springbootcfg.LogSaverCfg;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("experiment")
public class S3BucketReader implements ItemReader<Item> {


  private final Iterator<Item> items;

  private StepExecution stepExecution;


  public S3BucketReader(LogSaverCfg cfg, S3BucketClient s3Service) {


    // List<String> apps = s3Service.findSubFolders("logs/ecs/");
    items = new ArrayList().iterator();
    // s3Service.findItems(LogType.logs, apps, logDate).iterator();

  }

  @Override
  public Item read()
      throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

    if (items.hasNext()) {
      Item current = items.next();
      if (!items.hasNext()) {
        stepExecution.getExecutionContext().put("readerExhausted", Boolean.TRUE);
      }
      return current;
    }

    return null;
  }


  @BeforeStep
  public void setStepExecution(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }


}
