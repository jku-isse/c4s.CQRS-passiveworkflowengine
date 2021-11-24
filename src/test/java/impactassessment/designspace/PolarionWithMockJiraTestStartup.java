package impactassessment.designspace;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import artifactapi.jira.subtypes.IJiraIssueLink;
import at.jku.designspace.sdk.clientservice.PolarionInstanceService;
import impactassessment.api.Commands.*;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.EventList2Logger;
import impactassessment.query.NoOpHistoryLogEventLogger;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

public class PolarionWithMockJiraTestStartup {

	ReplayStatus status = ReplayStatus.REGULAR;
	String workflowId = "TestId1";
	String wft = "POLARION_TEST2";
	WorkflowDefinitionRegistry registry;
	WorkflowProjection wfp;
	IArtifactRegistry aRegistry;
	CommandGateway gw;
	
	@Before
	public void setup() {
		System.setProperty("org.jboss.logging.provider", "slf4j");
		Injector injector = DesignspaceDevelopmentConfig.getInjector();
		gw = injector.getInstance(CommandGateway.class);
		aRegistry = injector.getInstance(IArtifactRegistry.class);
		ProjectionModel pModel = new ProjectionModel(aRegistry);
		aRegistry.register(injector.getInstance(PolarionInstanceService.class));
		//aRegistry.register(DevelopmentConfig.getJiraDemoService());
		//aRegistry.register(DevelopmentConfig.getJiraService());
		aRegistry.register(injector.getInstance(IJiraService.class));
		IFrontendPusher fp = new SimpleFrontendPusher();
		IKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		registry = injector.getInstance(WorkflowDefinitionRegistry.class);
		wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry, new EventList2Logger(new NoOpHistoryLogEventLogger()));
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
	
	}
	
	@Test
	public void extractJiraLinks() {
		ArtifactIdentifier ai = new ArtifactIdentifier("DEMO-11", "IJiraArtifact");
		Optional<IArtifact> optArt = aRegistry.get(ai, workflowId);
		IJiraArtifact jira = (IJiraArtifact) optArt.get();
		Iterator<IJiraIssueLink> links = jira.getIssueLinks().iterator();
		while(links.hasNext()) {
			IJiraIssueLink link = links.next();
			String key = link.getTargetIssueKey();
			System.out.println(key);
		}
	}
	
	@Test
	public void runPolarionWithLiveJiraTest() {
		//ArtifactIdentifier ai = new ArtifactIdentifier("DEMOISSUE", "IJiraArtifact");
		ArtifactIdentifier ai = new ArtifactIdentifier("DEMO-10", "IJiraArtifact");
		Optional<IArtifact> optArt = aRegistry.get(ai, workflowId);
		IJiraArtifact jira = (IJiraArtifact) optArt.get();
		//Optional<String> descr = LoadTestHtmlFromFiles.load_ST24837incorrect_Description();
		//PolarionArtifact art = (PolarionArtifact) optArt.get();
		//art.getDescription();
		//art.getInstance().propertyAsSingle("description").set(descr.get());
//		art.getInstance().addProperty(property);
		Map<ArtifactIdentifier, String> input = new HashMap<>();
		input.put(ai,"jira");
		gw.send(new CreateWorkflowCmd(workflowId, input, wft));
		//kieS.getKieSession(workflowId).fireAllRules();
		//wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")), status);
		wfp.handle(new PrintKBQuery(workflowId));
		
	}

	
	
}
