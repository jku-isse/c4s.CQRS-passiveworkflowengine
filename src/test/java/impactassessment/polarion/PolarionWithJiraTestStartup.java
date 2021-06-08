package impactassessment.polarion;

import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import artifactapi.jira.IJiraArtifact;
import at.jku.designspace.sdk.clientservice.PolarionInstanceService;
import impactassessment.api.Events.CreatedWorkflowEvt;
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

public class PolarionWithJiraTestStartup {

	ReplayStatus status = ReplayStatus.REGULAR;
	String workflowId = "TestId1";
	String wft = "POLARION_TEST2";
	WorkflowDefinitionRegistry registry;
	WorkflowProjection wfp;
	PolarionInstanceService ps;
	IArtifactRegistry aRegistry;
	
	@Before
	public void setup() {
		System.setProperty("org.jboss.logging.provider", "slf4j");
		Injector injector = DevelopmentConfig.getInjector();
		CommandGateway gw = injector.getInstance(CommandGateway.class);
		aRegistry = injector.getInstance(IArtifactRegistry.class);
		ProjectionModel pModel = new ProjectionModel(aRegistry);
		ps = injector.getInstance(PolarionInstanceService.class);
		aRegistry.register(ps);
		aRegistry.register(DevelopmentConfig.getJiraDemoService());
		IFrontendPusher fp = new SimpleFrontendPusher();
		IKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		registry = injector.getInstance(WorkflowDefinitionRegistry.class);
		wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry);
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
	
	}
	
	@Test
	public void runPolarionWithJiraTest() {
		ArtifactIdentifier ai = new ArtifactIdentifier("DEMOISSUE", "IJiraArtifact");
		Optional<IArtifact> optArt = aRegistry.get(ai, workflowId);
		IJiraArtifact jira = (IJiraArtifact) optArt.get();
		//Optional<String> descr = LoadTestHtmlFromFiles.load_ST24837incorrect_Description();
		//PolarionArtifact art = (PolarionArtifact) optArt.get();
		//art.getDescription();
		//art.getInstance().propertyAsSingle("description").set(descr.get());
//		art.getInstance().addProperty(property);
//		
		wfp.on(new CreatedWorkflowEvt(workflowId, List.of(new AbstractMap.SimpleEntry<>("jira", ai)), wft, registry.get(wft).getWfd()), status);
		//kieS.getKieSession(workflowId).fireAllRules();
		//wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")), status);
		wfp.handle(new PrintKBQuery(workflowId));
		
	}

}
