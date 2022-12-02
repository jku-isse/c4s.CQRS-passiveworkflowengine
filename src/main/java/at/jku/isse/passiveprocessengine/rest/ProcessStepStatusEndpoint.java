package at.jku.isse.passiveprocessengine.rest;

import java.util.Optional;

import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;

@RestController
@CrossOrigin(origins = "*")//to allow incoming calls from other ports
public class ProcessStepStatusEndpoint {

	@Autowired
	RequestDelegate service;
		
	@GetMapping( value="/ceps/details", produces="application/json")
	public ResponseEntity<String> getProcessLogs(@QueryParam("processId") Long processId, @QueryParam("stepType") String stepType) { 
		if (service == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("{\"error\": \"No RequestDelegate available\"}");
		} else if (processId == null || processId <= 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"error\": \"processId parameter missing or invalid\"}");
		} else if (stepType == null || stepType.length() <= 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"error\": \"stepDefinitionId parameter missing or invalid\"}");
		} else {			
			Instance procInst = service.getWorkspace().findElement(Id.of(processId));
			if (procInst == null)
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("{\"error\": \"No process instance found with provided id\"}");
			try {
				ProcessInstance proc = WrapperCache.getWrappedInstance(ProcessInstance.class, procInst);
				if (proc != null) {
					Optional<ProcessStep> optStep = proc.getProcessSteps().stream().filter(step -> step.getDefinition().getName().equalsIgnoreCase(stepType)).findAny();
					if (optStep.isEmpty()) {
						return ResponseEntity.status(HttpStatus.NOT_FOUND)
								.body("No step found of provided type");
					} else {
						ProcessStep step = optStep.get();
						return ResponseEntity.status(HttpStatus.OK)
								.body("{\"id\" : \""+step.getName()+"\", "
										+"\"actualState\" : \""+step.getActualLifecycleState().toString()+"\","
										+"\"expectedState\" : \""+step.getExpectedLifecycleState().toString()+"\""
										+ " }");
					}
				}
			} catch(Exception e) {
				// ignored
			}
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body("{\"error\": \"Provided Id does not identifiy a process instance\"}");
		}
	}
	
//	@GetMapping( value="/ceps/status", produces="application/json")
//	public ResponseEntity<String> getProcessStatus(@QueryParam("processId") Long processId) { 
//		if (service == null) {
//			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//					.body("{\"error\": \"No RequestDelegate available\"}");
//		} else if (processId == null || processId <= 0) {
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//					.body("{\"error\": \"processId parameter missing or invalid\"}");
//		} else {			
//			Instance procInst = service.getWorkspace().findElement(Id.of(processId));
//			if (procInst == null)
//				return ResponseEntity.status(HttpStatus.NOT_FOUND)
//						.body("{\"error\": \"No process instance found with provided id\"}");
//			try {
//				ProcessInstance proc = WrapperCache.getWrappedInstance(ProcessInstance.class, procInst);
//				if (proc != null)
//					return ResponseEntity.status(HttpStatus.OK)
//						.body("{\"status\" : \"ok\" }");							
//			} catch(Exception e) {
//				// ignore
//			}
//			return ResponseEntity.status(HttpStatus.NOT_FOUND)
//					.body("{\"error\": \"Provided Id does not identifiy a process instance\"}");
//		}
//	}
	
	@GetMapping( value="/ceps/connectionstatus", produces="application/json")
	public ResponseEntity<String> getConnectionStatus() { 
		if (service == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("{\"error\": \"No RequestDelegate available\"}");
		} else return ResponseEntity.status(HttpStatus.OK)
						.body("{\"status\" : \"ok\" }");
	}
}
