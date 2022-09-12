package it.pagopa.pn.logsaver.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import it.pagopa.pn.logsaver.batch.processor.S3LogsProcessor;
import it.pagopa.pn.logsaver.batch.reader.S3BucketReader;
import it.pagopa.pn.logsaver.batch.writer.SafeStorageWriter;
import it.pagopa.pn.logsaver.model.ItemLog;

@Configuration
public class JobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;

  private final StepBuilderFactory stepBuilderFactory;

  private final SafeStorageWriter safeStorageWriter;

  private final S3BucketReader s3BucketReader;

  private final S3LogsProcessor s3LogsProcessor;

  public JobConfiguration(JobBuilderFactory jobBuilderFactory,
      StepBuilderFactory stepBuilderFactory, SafeStorageWriter safeStorageWriter,
      S3BucketReader s3BucketReader, S3LogsProcessor s3LogsProcessor) {
    this.jobBuilderFactory = jobBuilderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
    this.safeStorageWriter = safeStorageWriter;
    this.s3BucketReader = s3BucketReader;
    this.s3LogsProcessor = s3LogsProcessor;

  }

  @Bean
  public Job logSaverJob() {
    return jobBuilderFactory.get("job1").incrementer(new RunIdIncrementer()).flow(logSaverJobStep())
        .end().build();
  }

  private Step logSaverJobStep() {
    return stepBuilderFactory.get("step1").<ItemLog, ItemLog>chunk(1).reader(s3BucketReader)
        .processor(s3LogsProcessor).writer(safeStorageWriter).build();
  }

}
