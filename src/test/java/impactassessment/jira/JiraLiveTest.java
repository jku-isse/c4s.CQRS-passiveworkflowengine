package impactassessment.jira;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import impactassessment.DevelopmentConfig;
import impactassessment.api.Events.*;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

public class JiraLiveTest {

	ReplayStatus status = ReplayStatus.REGULAR;
	String workflowId = "TestId1";
	String wfd = "DEMOJIRA";
	

	WorkflowProjection wfp;
	ProjectionModel pModel;
	IKieSessionService kieS;
	IJiraService jiraS;
	WorkflowDefinitionRegistry wfdReg;
	IArtifactRegistry aRegistry;
	
	@Before
	public void setup() {
		System.setProperty("org.jboss.logging.provider", "slf4j");
		Injector inj =  DevelopmentConfig.getInjector();
		CommandGateway gw = inj.getInstance(CommandGateway.class);
		wfp = inj.getInstance(WorkflowProjection.class);
		pModel = inj.getInstance(ProjectionModel.class);
		aRegistry = inj.getInstance(IArtifactRegistry.class);
		IFrontendPusher fp = new SimpleFrontendPusher();
		jiraS = //inj.getInstance(IJiraService.class);
				DevelopmentConfig.getJiraService(false);
		wfdReg = inj.getInstance(WorkflowDefinitionRegistry.class);
		kieS = inj.getInstance(IKieSessionService.class);
		aRegistry.register(jiraS);
		((MockCommandGateway) gw).setWorkflowProjection(wfp);        
	}
	
	@Test
	public void runDemoJiraOnAtlassianTest() {
		wfp.on(new DeletedEvt(workflowId)); // to ensure any previous workflow is removed
		ArtifactIdentifier ai = new ArtifactIdentifier("DEMO-2", "IJiraArtifact");
		IArtifact art = aRegistry.get(ai, workflowId).get();
		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(ai, "req"), wfd, wfdReg.get(wfd).getWfd()), status);
		wfp.handle(new PrintKBQuery(workflowId));
	}
}
