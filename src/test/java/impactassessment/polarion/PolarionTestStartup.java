package impactassessment.polarion;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import at.jku.designspace.sdk.polarion.clientservice.interfaces.IPolarionService;
import at.jku.designspace.sdk.polarion.polarionapi.implementations.PolarionArtifact;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Events.UpdatedArtifactsEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

public class PolarionTestStartup {

	ReplayStatus status = ReplayStatus.REGULAR;
	String workflowId = "TestId1";
	String wft = "POLARION_TEST";
	WorkflowDefinitionRegistry registry;
	WorkflowProjection wfp;
	IPolarionService ps;
	
	@Before
	public void setup() {
		System.setProperty("org.jboss.logging.provider", "slf4j");
		Injector injector = DevelopmentConfig.getInjector();
		CommandGateway gw = injector.getInstance(CommandGateway.class);
		IArtifactRegistry aRegistry = injector.getInstance(IArtifactRegistry.class);
		ProjectionModel pModel = new ProjectionModel(aRegistry);
		ps = injector.getInstance(IPolarionService.class);
		aRegistry.register(ps);
		IFrontendPusher fp = new SimpleFrontendPusher();
		IKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry);
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
	
	}
	
	@Test
	public void runPolarionTest() {
		ArtifactIdentifier ai = new ArtifactIdentifier("DPS-511", "IPolarionArtifact");
		
//		art.getInstance().addProperty(property);
//		
		wfp.on(new CreatedWorkflowEvt(workflowId, List.of(new AbstractMap.SimpleEntry<>("workitem", ai)), wft, registry.get(wft).getWfd()), status);
		//kieS.getKieSession(workflowId).fireAllRules();
		//wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")), status);
		wfp.handle(new PrintKBQuery(workflowId));
		
		//now we update the artifact and see if the eval changes correctly
		Optional<IArtifact> optArt = ps.get(ai, workflowId);
		Optional<String> descr = LoadTestHtmlFromFiles.load_ST24837incorrect_Description();
		PolarionArtifact art = (PolarionArtifact) optArt.get();
		art.getDescription();
		art.getInstance().propertyAsSingle("description").set(descr.get());
		
		wfp.on(new UpdatedArtifactsEvt(workflowId, Collections.nCopies(1, ai)));
		
		wfp.handle(new PrintKBQuery(workflowId));
		
	}

}
