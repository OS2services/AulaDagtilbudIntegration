package dk.digitalidentity.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.service.TaskRunnerService;

@EnableScheduling
@Component
public class TaskRunner {

	@Autowired
	private TaskRunnerService service;

	@Scheduled(cron = "${configuration.cron}")
	public void run() {
		service.run();
	}
}
