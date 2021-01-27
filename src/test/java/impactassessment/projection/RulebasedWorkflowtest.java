package impactassessment.projection;

import java.util.AbstractMap;
import java.util.List;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.jira.IJiraArtifact;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.command.MockCommandGateway;

import org.axonframework.eventhandling.ReplayStatus;
import org.junit.jupiter.api.Test;

import com.google.inject.Injector;

import impactassessment.DevelopmentConfig;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.kiesession.KieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.MockWorkflowProjection;
import impactassessment.query.ProjectionModel;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;

class RulebasedWorkflowtest {

	KieSessionService kieS;
	
	
	@Test
	void test() {
		Injector injector = DevelopmentConfig.getInjector();
		MockCommandGateway gw = injector.getInstance(MockCommandGateway.class);
		ProjectionModel pModel = new ProjectionModel();
		IArtifactRegistry aRegistry = new ArtifactRegistry();
		JiraService jiraS = DevelopmentConfig.getJiraService();
		aRegistry.register(jiraS);
		aRegistry.register(injector.getInstance(JamaService.class));
		SimpleKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		WorkflowDefinitionRegistry registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		MockWorkflowProjection wfp = new MockWorkflowProjection(pModel, kieS,  gw, registry);
		gw.setWorkflowProjection(wfp);
		ReplayStatus status = ReplayStatus.REGULAR;
		String id = "TestId1";
		//new AddOutputCmd(id, id, null, "outDoc", new ArtifactType("IJiraArtifact") );
		
		IJiraArtifact jiraArt = (IJiraArtifact) jiraS.get(new ArtifactIdentifier("PVCSG-9", "IJiraArtifact"), id).get();
		wfp.on(new CreatedWorkflowEvt(id, List.of(new AbstractMap.SimpleEntry<>("jira",jiraArt)), "DemoProcess2", registry.get("DemoProcess2").getWfd()), status);
		//wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")), status);
		wfp.handle(new PrintKBQuery(id));
	}

}
