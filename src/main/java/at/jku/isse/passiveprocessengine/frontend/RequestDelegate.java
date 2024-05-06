package at.jku.isse.passiveprocessengine.frontend;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.passiveprocessengine.core.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.experiment.ProcessAccessControlProvider;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceError;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class RequestDelegate {

	@Autowired
	private ChangeEventTransformer changeEventTransformer;

	@Autowired
	UIConfig uiconfig;

	@Autowired
	ArtifactResolver resolver;

	@Autowired
	ProcessContext ws;

	@Autowired
	ProcessRegistry procReg;

	@Autowired IFrontendPusher frontend;

	@Autowired EventDistributor eventDistributor;

	@Autowired UsageMonitor monitor;

	@Autowired ProcessAccessControlProvider aclProvider;

	ProcessChangeListenerWrapper picp;

	private AtomicBoolean isInit = new AtomicBoolean(false);

	public RequestDelegate() {

	}

	@EventListener
	public void onApplicationEvent(ApplicationReadyEvent event) {
		initialize();
	}

	public void initialize() {
		if (isInit.compareAndExchange(false, true) == false) {

			//resolver.inject(ws); FIXME: put this into config/setup
			procReg.initProcessDefinitions();
			//		if (repAnalyzer != null)
			//			repAnalyzer.inject(ws);
			//		ArlRuleEvaluator arl = new ArlRuleEvaluator();
			//		arl.registerListener(repAnalyzer);
			//		RuleService.setEvaluator(arl);
			//RuleService.currentWorkspace = ws;		

			// load any persisted process instances
			Set<ProcessInstance> existingPI = procReg.loadPersistedProcesses();
			frontend.update(existingPI);	
			picp = new ProcessChangeListenerWrapper(ws, frontend, resolver, eventDistributor);
			changeEventTransformer.registerWithWorkspace(picp);
		}
	}

	public ProcessInstance instantiateProcess(String procName, Map<String, ArtifactIdentifier> inputs, String procDefinitionId ) throws ProcessException{
		initialize();

		Optional<ProcessDefinition> optProcDef = procReg.getProcessDefinition(procDefinitionId, true);
		if (optProcDef.isEmpty()) {
			String msg = String.format("ProcessDefinition %s not found", procDefinitionId);
			log.warn(msg);
			throw new ProcessException(msg);
		}

		List<ProcessException> pexs = new LinkedList<>();
		Map<String, Set<PPEInstance>> procInput = new HashMap<>();
		inputs.entrySet().stream()
		.forEach(entry -> {
			PPEInstance inst;
			try {
				inst = resolver.get(entry.getValue());
				procInput.put(entry.getKey(), Set.of(inst));
			} catch (ProcessException e) {
				pexs.add(e);
			}
		});
		if (pexs.size() > 0) {
			ProcessException pe = new ProcessException("Error resolving artifacts");
			pexs.stream().forEach(pex -> pe.getErrorMessages().add(pex.getMainMessage()));
			throw pe;
		}
		SimpleEntry<ProcessInstance,List<ProcessInstanceError>> pd = procReg.instantiateProcess(optProcDef.get(), procInput);
		if (pd.getValue().isEmpty())
			return pd.getKey();
		else
			throw new ProcessException(pd.getValue().toString());	
	}

	public void addProcessInput(ProcessInstance pInst, String inParam, String artId, String artIdType) throws ProcessException{
		PPEInstance inst = resolver.get(new ArtifactIdentifier(artId, artIdType));
		IOResponse resp = pInst.addInput(inParam, inst);
		if (resp.getError() != null) {
			throw new ProcessException(resp.getError());
		} else {
			ws.getInstanceRepository().concludeTransaction();
		}
	}

	private void addIO(boolean isInput, String procId, String stepId, String param, String artId, String artType) throws ProcessException {
		initialize();
		ProcessException pex = new ProcessException("Error adding i/o of step: "+stepId);
		//	ProcessInstance pi = pInstances.get(procId);
		ProcessInstance pi = procReg.getProcess(procId);
		if (pi != null) {
			pi.getProcessSteps().stream()
			.filter(step -> step.getName().equals(stepId))
			.findAny().ifPresent(step -> {
				try {
					PPEInstance inst = resolver.get(new ArtifactIdentifier(artId, artType));
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
		else {
			ws.getInstanceRepository().concludeTransaction();
			//ws.commit();
			//fetchLazyLoaded();
		}
	}

	public void addInput(String procId, String stepId, String param, String artId, String artType) throws ProcessException {
		initialize();
		addIO(true, procId, stepId, param, artId, artType);
	}

	public void addOutput(String procId, String stepId, String param, String artId, String artType) throws ProcessException {
		initialize();
		addIO(false, procId, stepId, param, artId, artType);
	}

	public void removeInputFromProcess(ProcessInstance proc, String inParam, String artId) {
		Set<PPEInstance> inputs = proc.getInput(inParam);
		inputs.stream()
		.filter(art -> art.getName().equalsIgnoreCase(artId))
		.findAny()
		.ifPresent(art -> {
			proc.removeInput(inParam, art);
			ws.getInstanceRepository().concludeTransaction();
		});

	}

	public ProcessInstance getProcess(String id) {
		return procReg.getProcess(id);
		//return pInstances.get(id);
	}

	public void deleteProcessInstance(String id) {
		initialize();
		frontend.remove(id);
		procReg.removeProcess(id);
	}

	public int resetAndUpdate() { // just a quick hack for now.
		return picp.resetAndUpdate();
	}
	
	public ProcessChangeListenerWrapper getProcessChangeListenerWrapper() {
		// to enable automatic lazyloading from ARL playground
		return picp;
	}
	
	
	public UIConfig getUIConfig() {
		return uiconfig;
	}

	//	public void ensureConstraintStatusConsistency(ProcessInstance proc) {
	//		log.info("ConstraintStatus Consistency check requested for process instance: "+proc.getName());
	//		List<ProcessScopedCmd> incons = proc.ensureRuleToStateConsistency();
	//		picp.executeConstraintStateInconsistencyRepairingCommands(incons);
	//	}

	public ProcessAccessControlProvider getACL() {
		return aclProvider;
	}

	public ProcessContext getProcessContext() {
		return ws;
	}

	public ProcessRegistry getRegistry() {
		initialize();
		return procReg;
	}

	public ArtifactResolver getArtifactResolver() {
		initialize();
		return resolver;
	}

	public UsageMonitor getMonitor() {
		return monitor;
	}

	public IFrontendPusher getFrontendPusher() {
		return frontend;
	}

	public EventDistributor getEventDistributor() {
		return eventDistributor;
	}


}
