package at.jku.isse.passiveprocessengine.frontend.artifacts;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Tool;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.endpoints.grpc.service.ServiceResponse;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import lombok.extern.slf4j.Slf4j;

@Component
public class DemoServiceWrapper implements IArtifactProvider{
	
	protected Workspace ws;
	protected InstanceType demoType;
	boolean isSimulatorRunning = false;

	
	@Override
	public ServiceResponse getServiceResponse(String id, String service) {
		if (demoType != null) {
			startUpdateSimulator();
			@SuppressWarnings("unchecked")
			Optional<Id> optInst = demoType.getInstancesIncludingThoseOfSubtypes().get().stream()
				.filter(inst -> ((Element<Instance>) inst).name().equalsIgnoreCase(id))
				.map(inst -> ((Element<Instance>) inst).id())
				.findAny();
			if (optInst.isPresent()) {
				return new ServiceResponse(0, service, "Success", ""+optInst.get().value());
			}
			else 
				return new ServiceResponse(2, service, "Not Found", "");
		}
		return new ServiceResponse(3, service, "Service Not Initialized", "");
	}

	@Override
	public boolean isProviding(String artifactType) {
		return artifactType.equalsIgnoreCase(TestArtifacts.DEMOISSUETYPE);
	}

    @EventListener
    public void onApplicationEvent(ApplicationStartedEvent event) {
//    	Tool tool = new Tool("PPEv3-DemoIssueProvider", "v1.0");
		ws = WorkspaceService.PUBLIC_WORKSPACE;
    	demoType = TestArtifacts.getJiraInstanceType(ws);
    	Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
    }
	
    private void startUpdateSimulator() {
    	if (!isSimulatorRunning) {
    		long initialDelayInSeconds = 10;
    		long intervalInSeconds = 30;
    		UpdateSimulator.scheduler.scheduleAtFixedRate(new UpdateSimulator(ws, demoType), initialDelayInSeconds, intervalInSeconds, java.util.concurrent.TimeUnit.SECONDS);
    		isSimulatorRunning = true;
    	}
    }
    
    @Slf4j
    private static class UpdateSimulator implements Runnable {

    	protected final static ScheduledExecutorService scheduler =
   		     Executors.newScheduledThreadPool(1);	
    	
    	Workspace ws;
    	InstanceType demoType;
    	public UpdateSimulator(Workspace ws, InstanceType demoType) {
    		log.debug("Started");
    		this.ws = ws;
    		this.demoType = demoType;
    	}
    	
		@Override
		public void run() {
			List<Instance> instances = (List<Instance>) demoType.getInstancesIncludingThoseOfSubtypes().get().stream()
					.map(inst -> (Instance) inst)
					.collect(Collectors.toList());
			if (!instances.isEmpty()) {
				Collections.shuffle(instances);
				Instance inst = instances.get(0);
				JiraStates state = TestArtifacts.getState(inst);
				switch(state) {
				case Closed:
					TestArtifacts.setStateToJiraInstance(inst, JiraStates.Open);
					break;
				case InProgress:
					TestArtifacts.setStateToJiraInstance(inst, JiraStates.Closed);
					break;
				case Open:
					TestArtifacts.setStateToJiraInstance(inst, JiraStates.InProgress);
					break;
				default:
					TestArtifacts.setStateToJiraInstance(inst, JiraStates.Open);
					break;
				}
				log.debug("Updating: "+inst.name());
				ws.commit();
			}
		}
    	
    }
    
}
