package impactassessment.projection;

import java.util.Collections;

import artifactapi.IArtifactRegistry;
import artifactapi.IArtifactService;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.artifactconnector.jira.JiraJsonService;
import org.axonframework.eventhandling.ReplayStatus;
import org.junit.jupiter.api.Test;

import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.kiesession.KieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;

class RulebasedWorkflowtest {

	KieSessionService kieS;
	
	
	@Test
	void test() {
		
		ProjectionModel pModel = new ProjectionModel();
		MockCommandGateway gw = new MockCommandGateway();
		IArtifactService aService = new JiraJsonService(new JiraChangeSubscriber(gw));
		IArtifactRegistry aRegistry = new ArtifactRegistry();
		aRegistry.register(aService);
		SimpleKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		WorkflowDefinitionRegistry registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		MockWorkflowProjection wfp = new MockWorkflowProjection(pModel, kieS,  gw, registry);
		gw.setWorkflowProjection(wfp);
		
		wfp.on(new CreatedWorkflowEvt("TestId1", Collections.emptyList(), "LINEARWITHDATA_WORKFLOW_TYPE", registry.get("LINEARWITHDATA_WORKFLOW_TYPE").getWfd()), ReplayStatus.REGULAR);
	}

}
