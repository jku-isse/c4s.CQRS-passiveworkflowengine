package at.jku.isse.passiveprocessengine.frontend;

import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.RuleEvaluationService;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.experiment.ProcessAccessControlProvider;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import at.jku.isse.passiveprocessengine.rest.ProcessCreationEndpoint;
import at.jku.isse.passiveprocessengine.rest.ProcessCreationEndpoint.CreateRequest;
import at.jku.isse.passiveprocessengine.rest.ProcessCreationEndpoint.ErrorResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class RequestDelegate {

	@Autowired ProcessCreationEndpoint processEndpoint;
	
	@Autowired @Getter RuleEvaluationService ruleEvaluationService;
	@Autowired @Getter	UIConfig uiConfig;
	@Autowired @Getter ArtifactResolver artifactResolver;
	@Autowired @Getter ProcessContext processContext;
	@Autowired @Getter ProcessRegistry processRegistry;
	@Autowired @Getter IFrontendPusher frontendPusher;
	@Autowired @Getter EventDistributor eventDistributor;
	@Autowired @Getter UsageMonitor usageMonitor;
	@Autowired @Getter ProcessAccessControlProvider aclProvider;
	@Autowired @Getter ProcessChangeListenerWrapper processChangeListenerWrapper;
	
	Gson gson = new GsonBuilder().create();
	
	public RequestDelegate() {

	}

	@EventListener
	public void onApplicationEvent(ApplicationReadyEvent event) {
		frontendPusher.update(processRegistry.getNonDeletedProcessInstances());	
	}

	public ProcessInstance instantiateProcess(String procName, Map<String, ArtifactIdentifier> inputs, String procDefinitionId ) throws ProcessException{		
		CreateRequest createRequest = new CreateRequest(procName, inputs, procDefinitionId);
		ResponseEntity<String> response = processEndpoint.createProcess(createRequest);
		if (response.getStatusCode().isError()) {
			ErrorResponse errResp = gson.fromJson(response.getBody(), ErrorResponse.class);
			ProcessException pex = new ProcessException(errResp.getMainMessage(), errResp.getErrorMessages());
			throw pex;
		} else {
			JsonObject jsonProc = JsonParser.parseString(response.getBody()).getAsJsonObject();					
			String name = jsonProc.getAsJsonPrimitive("name").getAsString();
			return processRegistry.getProcessByName(name);
		}				
	}

	public void addProcessInput(ProcessInstance pInst, String inParam, String artId, String artIdType) throws ProcessException{
		PPEInstance inst = artifactResolver.get(new ArtifactIdentifier(artId, artIdType));
		IOResponse resp = pInst.addInput(inParam, inst);
		if (resp.getError() != null) {
			throw new ProcessException(resp.getError());
		} else {
			processContext.getInstanceRepository().concludeTransaction();
		}
	}

	public void removeInputFromProcess(ProcessInstance proc, String inParam, String artId) {
		Set<PPEInstance> inputs = proc.getInput(inParam);
		inputs.stream()
		.filter(art -> art.getName().equalsIgnoreCase(artId))
		.findAny()
		.ifPresent(art -> {
			proc.removeInput(inParam, art);
			processContext.getInstanceRepository().concludeTransaction();
		});

	}

	public ProcessInstance getProcess(String id) {
		return processRegistry.getProcessByName(id);
		//return pInstances.get(id);
	}

	public void deleteProcessInstance(String id) {		
		frontendPusher.remove(id);
		processRegistry.removeProcessByName(id);
	}

}
