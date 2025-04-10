package at.jku.isse.passiveprocessengine.demo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.rdfwrapper.ArtifactRepository;
import at.jku.isse.passiveprocessengine.rdfwrapper.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.ArtifactIdentifier;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.FetchResponse;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.IArtifactProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.FetchResponse.ErrorResponse;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.FetchResponse.SuccessResponse;
import lombok.extern.slf4j.Slf4j;

public class DemoArtifactProvider extends ArtifactRepository implements IArtifactProvider {


	private final PPEInstanceType demoType;
	private final TestArtifacts artifactFactory;
	private final InstanceRepository ws;
	private boolean isSimulatorRunning = false;
	
	public DemoArtifactProvider(SchemaRegistry schemaReg, InstanceRepository ws, TestArtifacts artifactFactory ) {
		super(artifactFactory.getJiraInstanceType(), schemaReg.getTypeByName(CoreTypeFactory.BASE_TYPE_URI) , ws);
		demoType = artifactFactory.getJiraInstanceType();
		this.artifactFactory = artifactFactory;
		this.ws = ws;
	}
	
	@Override
	public Map<PPEInstanceType, List<String>> getSupportedIdentifiers() {
		return Map.of(getDefaultArtifactInstanceType(), List.of(demoType.getName()));
	}
	
	@Override
	public PPEInstanceType getDefaultArtifactInstanceType() {
		return demoType;
	}

	@Override
	public Set<PPEInstanceType> getProvidedArtifactInstanceTypes() {
		return Set.of(demoType);				
	}

	@Override
	public Set<FetchResponse> fetchArtifact(Set<ArtifactIdentifier> artifactIdentifiers) {
		startUpdateSimulator();
		return artifactIdentifiers.stream()
			.map(id -> {
				Optional<PPEInstance> optInst = Optional.ofNullable(super.getInstanceByExternalDefaultId(id.getId()));
				if (optInst.isEmpty()) {
					return new ErrorResponse("No DemoIssue found for id: "+Objects.toString(id));
				} else {
					return new SuccessResponse(optInst.get());
				}
			})
			.collect(Collectors.toSet());
	}

	@Override
	public Set<FetchResponse> forceFetchArtifact(Set<ArtifactIdentifier> artifactIdentifiers) {
		return fetchArtifact(artifactIdentifiers);
	}
	
    private void startUpdateSimulator() {
    	if (!isSimulatorRunning) {
    		isSimulatorRunning = true;
    		
    		long initialDelayInSeconds = 10;
    		long intervalInSeconds = 30;
    		UpdateSimulator.scheduler.scheduleAtFixedRate(new UpdateSimulator(this, artifactFactory, ws), initialDelayInSeconds, intervalInSeconds, java.util.concurrent.TimeUnit.SECONDS);
    	}
    }
    
    @Slf4j
    private static class UpdateSimulator implements Runnable {

    	protected final static ScheduledExecutorService scheduler =
   		     Executors.newScheduledThreadPool(1);	
    	
    	final ArtifactRepository repo;
    	final TestArtifacts artifactFactory;
    	final InstanceRepository ws;
    	
    	public UpdateSimulator(ArtifactRepository repo, TestArtifacts artifactFactory, InstanceRepository ws) {
    		log.debug("Started");
    		this.repo = repo;
    		this.artifactFactory = artifactFactory;
    		this.ws = ws;
    	}
    	
		@Override
		public void run() {
			var instances = repo.getAllInstances().stream().toList();
			if (!instances.isEmpty()) {
				Collections.shuffle(instances);
				PPEInstance inst = instances.get(0);
				JiraStates state = TestArtifacts.getState(inst);
				switch(state) {
				case Closed:
					artifactFactory.setStateToJiraInstance(inst, JiraStates.Open);
					break;
				case InProgress:
					artifactFactory.setStateToJiraInstance(inst, JiraStates.Closed);
					break;
				case Open:
					artifactFactory.setStateToJiraInstance(inst, JiraStates.InProgress);
					break;
				default:
					artifactFactory.setStateToJiraInstance(inst, JiraStates.Open);
					break;
				}
				log.debug("Updated: "+inst.getName());
				ws.concludeTransaction();
			}
		}
    	
    }
	
}
