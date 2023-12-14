package at.jku.isse.passiveprocessengine.rest;

import java.net.URI;
import java.util.Optional;

import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")//to allow incoming calls from other ports
public class ProcessStepStatusEndpoint {

	@Autowired
	RequestDelegate service;
		
	@GetMapping( value="/ceps/details", produces="application/json")
	public ResponseEntity<String> getProcessLogs(@QueryParam("processId") Long processId, @QueryParam("stepType") String stepType) {		
		if (service == null) {
			log.info("No RequestDelegate available");
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("{\"error\": \"No RequestDelegate available\"}");
		} else if (processId == null || processId <= 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"error\": \"processId parameter missing or invalid\"}");
		} else if (stepType == null || stepType.length() <= 0) {
			log.info("ProcessID parameter missing or invalid");
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
								.body("{\"error\": \"No step found of provided type\"}");
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
	
	@GetMapping( value="/ceps/qadetails", produces="application/json")
	public ResponseEntity<String> getQAStatus(@QueryParam("processId") Long processId, @QueryParam("document") String document) { 
		if (service == null) {
			log.info("No RequestDelegate available");
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)					
					.body("{\"error\": \"No RequestDelegate available\"}");
		} else if (processId == null || processId <= 0) {
			log.info("ProcessID parameter missing or invalid"); // actually we no ignore process parameter and encode process in document identifier
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"error\": \"processId parameter missing or invalid\"}");
		} else if (document == null || document.length() <= 0) {
			log.info("Document parameter missing or invalid");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"error\": \"document parameter missing or invalid\"}");
		} else {			
			int sepPos = document.indexOf("::");
			if (sepPos == -1) {
				log.info("Document parameter has no process/doc separation token '::' ");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"error\": \"Document parameter has no process/doc separation token\"}");
			}
			String procId = document.substring(0, sepPos);
			try {
				//	now lets override processId			
				processId = Long.parseLong(procId);
				// now lets override document
				String documentStr = document.substring(sepPos+2);
				Instance procInst = service.getWorkspace().findElement(Id.of(processId));
				if (procInst == null) {
					log.info(String.format("Process with id %s not found", processId));
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body("{\"error\": \"No process instance found with provided id\"}");
				}			
				ProcessInstance proc = WrapperCache.getWrappedInstance(ProcessInstance.class, procInst);
				if (proc != null) {
					Optional<ConstraintWrapper> cwOpt = proc.getProcessSteps().stream()
						.flatMap(step -> step.getQAstatus().stream()) // just go through all constraint wrappers one by one
						.filter(cw1 -> cw1.getSpec().getName().equalsIgnoreCase(documentStr))
						.findAny();
					if (cwOpt.isEmpty()) {
						log.info(String.format("Constraint %s not found for process %s", documentStr, processId));
						return ResponseEntity.status(HttpStatus.NOT_FOUND)
								.body("{\"error\": \"No constraint found for the provided document name\"}");
					} 					
					else {
						ConstraintWrapper cw = cwOpt.get();
						String status = "";
						if (cw.getCr()==null) {
							status = "draft";//"NOT_YET_EVALUATED";															
						} else {
							status = cw.getEvalResult() ? "released" : "not existent" ;//"FULFILLED" : "UNFULFILLED";
						}			
						
						String link = getConstraintURI(proc.getInstance().id().toString(), cw.getInstance().id().toString()) ;
						
						log.debug(String.format("Process %s constraint %s returned as '%s'", processId, document, status));
						return ResponseEntity.status(HttpStatus.OK)
									.body("{\"id\" : \""+cw.getName()+"\", "
											+"\"evaluationStatus\" : \""+status+"\", "
											+"\"link\" : \""+link+"\""
											+ " }");
					}
				}
			} catch(Exception e) {
				// ignored
				log.debug(e.getMessage());
			}
			log.info(String.format("Provided id %s does not identify a process", processId));
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body("{\"error\": \"Provided Id does not identifiy a process instance\"}");
		}
	}
		
	private String getConstraintURI(String procId, String cwId) {
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();						
		URI uri = builder.build().toUri();
		StringBuffer sb = new StringBuffer(	uri.getScheme()+"://"+uri.getHost() );
		if (uri.getPort() > 0) {
			sb.append(":");
			sb.append(uri.getPort());
		}
		sb.append("/home/?id="+procId+"&focus="+cwId);
		return sb.toString();
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
