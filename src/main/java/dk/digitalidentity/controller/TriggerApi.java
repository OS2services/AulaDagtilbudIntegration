package dk.digitalidentity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.service.TaskRunnerService;

@RestController
public class TriggerApi {

	@Autowired
	private TaskRunnerService service;

	@GetMapping("/api/trigger")
	public ResponseEntity<?> trigger() {
		service.run();
		
		return ResponseEntity.ok().build();
	}
}
