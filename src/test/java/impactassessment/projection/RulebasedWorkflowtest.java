package impactassessment.projection;

import java.util.Collections;

import org.axonframework.eventhandling.ReplayStatus;
import org.junit.jupiter.api.Test;

import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.JiraChangeSubscriber;
import impactassessment.jiraartifact.JiraJsonService;
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
		IJiraArtifactService aService = new JiraJsonService(new JiraChangeSubscriber(gw));
		SimpleKieSessionService kieS = new SimpleKieSessionService(gw, aService);
		WorkflowDefinitionRegistry registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		MockWorkflowProjection wfp = new MockWorkflowProjection(pModel, kieS,  gw, registry);
		gw.setWorkflowProjection(wfp);
		
		wfp.on(new CreatedWorkflowEvt("TestId1", Collections.emptyList(), "LINEARWITHDATA_WORKFLOW_TYPE", registry.get("LINEARWITHDATA_WORKFLOW_TYPE").getWfd()), ReplayStatus.REGULAR);
	}

}
