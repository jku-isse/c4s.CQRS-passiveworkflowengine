package at.jku.isse.passiveprocessengine.frontend;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import artifactapi.ArtifactIdentifier;
import at.jku.isse.PPEv3Frontend;
import at.jku.isse.designspace.artifactconnector.core.IService;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Tool;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.commands.Responses.InputResponse;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class RequestDelegate implements IService{

	
	Workspace ws;

	@Autowired
	ArtifactResolver resolver;
	
	@Autowired
	ProcessRegistry procReg;
	
	ProcessInstanceChangeProcessor picp;
	Map<String, ProcessInstance> pInstances = new HashMap<>();
	
	boolean isInitialized = false;
	
	public RequestDelegate() {
		PPEv3Frontend.SERVICES_TO_INITIALIZE.add(this);
	}
	
	public ProcessRegistry getRegistry() {
		if (!isInitialized) initialize();
		return procReg;
	}
	
	public void instantiateProcess(String procName, Map<String, ArtifactIdentifier> inputs, String procDefinitionId ) throws ProcessException{
		if (!isInitialized) initialize();
		
		Optional<ProcessDefinition> optProcDef = procReg.getProcessDefinition(procDefinitionId);
		if (optProcDef.isEmpty()) {
			String msg = String.format("ProcessDefinition %s not found", procDefinitionId);
			log.warn(msg);
			throw new ProcessException(msg);
		}
	
		List<ProcessException> pexs = new LinkedList<>();
		Map<String, Instance> procInput = new HashMap<>();
		inputs.entrySet().stream()
				.forEach(entry -> {
						Instance inst;
						try {
							inst = resolver.get(entry.getValue());
							procInput.put(entry.getKey(), inst);
						} catch (ProcessException e) {
							pexs.add(e);
						}
				});
		if (pexs.size() > 0) {
			ProcessException pe = new ProcessException("Error resolving artifacts");
			pexs.stream().forEach(pex -> pe.getErrorMessages().add(pex.getMainMessage()));
			throw pe;
		}
		
		ProcessInstance pInst = ProcessInstance.getInstance(ws, optProcDef.get());
		List<InputResponse> errResp = procInput.entrySet().stream()
			.map(entry -> pInst.addInput(entry.getKey(), entry.getValue()))
			.filter(resp -> resp.getError() != null)
			.collect(Collectors.toList());
		if (errResp.isEmpty()) {
			pInstances.put(pInst.getName(), pInst);
		} else {
			ProcessException ex = new ProcessException("Unable to instantiate process");
			errResp.stream().forEach(err -> ex.getErrorMessages().add(err.getError()));
			throw ex;
		}
	}
	
	public void addInput(String procId, String stepId, String param, String artId, String artType) {
		if (!isInitialized) initialize();
	}
	
	public void addOutput(String procId, String stepId, String param, String artId, String artType) {
		if (!isInitialized) initialize();
	}
	
	public void deleteProcessInstance(String id) {
		if (!isInitialized) initialize();
		
		ProcessInstance pi = pInstances.remove(id);
		if (pi != null) {
			pi.deleteCascading();
		}
		
	}


	
	public void initialize() {
		Tool tool = new Tool("PPEv3", "v1.0");
		ws = WorkspaceService.createWorkspace("PPEv3", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, tool, false, false);
		resolver.inject(ws);
		procReg.inject(ws);
		RuleService.setEvaluator(new ArlRuleEvaluator());
		picp = new ProcessInstanceChangeProcessor(ws);
		isInitialized = true;
	}
}
