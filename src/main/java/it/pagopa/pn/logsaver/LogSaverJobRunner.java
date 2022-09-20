package it.pagopa.pn.logsaver;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("experiment")
@EnableBatchProcessing
public class LogSaverJobRunner implements ApplicationRunner {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job logSaverJob;


  @Autowired
  private ConfigurableApplicationContext ctx;



  private int lunnchJob() throws JobExecutionAlreadyRunningException, JobRestartException,
      JobInstanceAlreadyCompleteException, JobParametersInvalidException {

    JobExecution jobExecution =
        jobLauncher.run(logSaverJob, new JobParametersBuilder().toJobParameters());

    log.debug("Batch job ends with status as {}", jobExecution.getStatus());
    return BatchStatus.COMPLETED == jobExecution.getStatus() ? 0 : 1;
  }


  @Override
  public void run(ApplicationArguments args) throws Exception {

    SpringApplication.exit(ctx, () -> {
      try {
        return lunnchJob();
      } catch (JobExecutionAlreadyRunningException | JobRestartException
          | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {

        return 1;
      }
    });

  }


}

