package at.jku.isse.passiveprocessengine.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.RepairAnalyzer;

@RestController
@CrossOrigin(origins = "*")//to allow incoming calls from other ports
public class ProcessMonitoringEndpoint {

	@Autowired
	RepairAnalyzer repAnalyzer;
	
	@Autowired
	ProcessQAStatsMonitor qaMonitor;
	
	@GetMapping( value="repairstatistics", produces="application/json")
	public ResponseEntity<String> getRepairAnalysisStatistics() { 
		if (repAnalyzer == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("No RepairAnalysis available");
		} else
		return ResponseEntity.status(HttpStatus.OK)
				.body(repAnalyzer.stats2Json(repAnalyzer.getSerializableStats()));
	}
	
	@DeleteMapping( value="repairstatistics", produces="application/json")
	public ResponseEntity<String> resetRepairAnalysisStatistics() { 
		if (repAnalyzer == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("No RepairAnalysis available");
		} else {
			repAnalyzer.reset();
		return ResponseEntity.status(HttpStatus.OK)
				.body("{\"result\" : \"Repair Statistics successfully cleared\" }");
		}
	}
	
	@GetMapping( value="qastatistics", produces="application/json")
	public ResponseEntity<String> getQAStatistics() { 
		if (qaMonitor == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("{\"result\" : \"No QAStatistics available\" }");
		} else {
			qaMonitor.calcFinalStats();
			return ResponseEntity.status(HttpStatus.OK)
				.body(qaMonitor.stats2Json(qaMonitor.stats.values()));
		}
	}
	
	@DeleteMapping( value="qastatistics", produces="application/json")
	public ResponseEntity<String> resetQAStatistics() { 
		if (qaMonitor == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("{\"result\" : \"No QAStatistics available\" }");
		} else {
			qaMonitor.reset();
		return ResponseEntity.status(HttpStatus.OK)
				.body("{\"result\" : \"QAStatistics successfully cleared\" }");
		}
	}
}
