package impactassessment.projection;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.jira.IJiraArtifact;
import com.google.inject.Injector;
import impactassessment.DevelopmentConfig;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.EventList2Forwarder;
import impactassessment.query.NoOpHistoryLogEventLogger;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.SimpleFrontendPusher;
import org.axonframework.eventhandling.ReplayStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

class RulebasedWorkflowtest {

	IKieSessionService kieS;
	
	
	@Test
	void test() {
		Injector injector = DevelopmentConfig.getInjector();
		MockCommandGateway gw = injector.getInstance(MockCommandGateway.class);
		IArtifactRegistry aRegistry = new ArtifactRegistry();
		ProjectionModel pModel = new ProjectionModel(aRegistry);
		IJiraService jiraS = DevelopmentConfig.getJiraService(false);
		aRegistry.register(jiraS);
		aRegistry.register(injector.getInstance(JamaService.class));
		SimpleKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		WorkflowDefinitionRegistry registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		SimpleFrontendPusher fp = new SimpleFrontendPusher();
		WorkflowProjection wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry, new EventList2Forwarder());
		gw.setWorkflowProjection(wfp);
		ReplayStatus status = ReplayStatus.REGULAR;
		String id = "TestId1";
		//new AddOutputCmd(id, id, null, "outDoc", new ArtifactType("IJiraArtifact") );
		
		IJiraArtifact jiraArt = (IJiraArtifact) jiraS.get(new ArtifactIdentifier("PVCSG-9", "IJiraArtifact"), id).get();
		wfp.on(new CreatedWorkflowEvt(id, Map.of(new ArtifactIdentifier("PVCSG-9", "IJiraArtifact"), "jira"), "DemoProcess2", registry.get("DemoProcess2").getWfd()), status);
		//wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")), status);
		wfp.handle(new PrintKBQuery(id));
	}

}
