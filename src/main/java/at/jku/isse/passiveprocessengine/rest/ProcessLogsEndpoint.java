package at.jku.isse.passiveprocessengine.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import at.jku.isse.passiveprocessengine.monitoring.ProcessStateChangeLog;

@RestController
@CrossOrigin(origins = "*")//to allow incoming calls from other ports
public class ProcessLogsEndpoint {

	@Autowired
	ProcessStateChangeLog logs;

	
	@GetMapping( value="processlogs/{id}", produces="application/json")
	public ResponseEntity<String> getProcessLogs(@PathVariable("id") Long id) { 
		if (logs == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("No ProcessLogs available");
		} else {
			String log = logs.getEventLogAsJson(id);
			if (log == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{}");
			} else
				return ResponseEntity.status(HttpStatus.OK)
						.body(log);
		}
	}
	
	
}
