package com.moebius.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultJobListener extends JobExecutionListenerSupport {
	@Override
	public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			log.info("Job has been completed. [{}]", jobExecution);
		}
		super.afterJob(jobExecution);
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		super.beforeJob(jobExecution);
	}
}
