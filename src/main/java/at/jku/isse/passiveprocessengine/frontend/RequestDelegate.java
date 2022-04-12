package at.jku.isse.passiveprocessengine.frontend;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Tool;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class RequestDelegate {

	
	Workspace ws;

	@Autowired
	ArtifactResolver resolver;
	
	@Autowired
	ProcessRegistry procReg;
	
	@Autowired IFrontendPusher frontend;
	
	ProcessInstanceChangeProcessor picp;
	Map<String, ProcessInstance> pInstances = new HashMap<>();
	
	boolean isInitialized = false;
	
	public RequestDelegate() {
		
	}
	
    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
       initialize();
    }
	
	public ProcessRegistry getRegistry() {
		if (!isInitialized) initialize();
		return procReg;
	}
	
	public Workspace getWorkspace() {
		if (!isInitialized) initialize();
		return ws;
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
		List<IOResponse> errResp = procInput.entrySet().stream()
			.map(entry -> pInst.addInput(entry.getKey(), entry.getValue()))
			.filter(resp -> resp.getError() != null)
			.collect(Collectors.toList());
		ws.commit();
		if (errResp.isEmpty()) {
			pInstances.put(pInst.getName(), pInst);
			frontend.update(pInst);
		} else {
			ProcessException ex = new ProcessException("Unable to instantiate process");
			errResp.stream().forEach(err -> ex.getErrorMessages().add(err.getError()));
			throw ex;
		}
	}
	
	private void addIO(boolean isInput, String procId, String stepId, String param, String artId, String artType) throws ProcessException {
		if (!isInitialized) initialize();
		ProcessException pex = new ProcessException("Error adding i/o of step: "+stepId);
		ProcessInstance pi = pInstances.get(procId);
		if (pi != null) {
			pi.getProcessSteps().stream()
				.filter(step -> step.getName().equals(stepId))
				.findAny().ifPresent(step -> {
					try {
						Instance inst = resolver.get(new ArtifactIdentifier(artId, artType));
						IOResponse resp = isInput ? step.addInput(param, inst) : step.addOutput(param, inst);
						if (resp.getError() != null)
							pex.getErrorMessages().add(resp.getError());
					} catch (ProcessException e) {
						pex.getErrorMessages().add(e.getMessage());
					}
				});
		} else {
			pex.getErrorMessages().add("Process not found by id: "+procId);
		}
		if (pex.getErrorMessages().size() > 0)
			throw pex;
		else
			ws.concludeTransaction();
	}
	
	public void addInput(String procId, String stepId, String param, String artId, String artType) throws ProcessException {
		if (!isInitialized) initialize();
		addIO(true, procId, stepId, param, artId, artType);
	}
	
	public void addOutput(String procId, String stepId, String param, String artId, String artType) throws ProcessException {
		if (!isInitialized) initialize();
		addIO(false, procId, stepId, param, artId, artType);
	}
	
	public void deleteProcessInstance(String id) {
		if (!isInitialized) initialize();
		frontend.remove(id);
		ProcessInstance pi = pInstances.remove(id);
		if (pi != null) {
			pi.deleteCascading();
			ws.concludeTransaction();
		}
	}

	public void initialize() {
		Tool tool = new Tool("PPEv3", "v1.0");
		ws = WorkspaceService.createWorkspace("PPEv3", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, tool, true, false);
		resolver.inject(ws);
		procReg.inject(ws);
		RuleService.setEvaluator(new ArlRuleEvaluator());
		picp = new ProcessInstanceChangeProcessor(ws);
		isInitialized = true;
	}
}
