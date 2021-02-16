package c4s.qualityassurance.dev;

import artifactapi.IArtifactRegistry;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.command.MockCommandGateway;

import org.axonframework.commandhandling.gateway.CommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

class CompileTest {

	

	public static void main(String[] args) {
		CommandGateway gw = new MockCommandGateway();
		ProjectionModel pModel = new ProjectionModel();
		IArtifactRegistry aRegistry = new ArtifactRegistry();
		IFrontendPusher fp = new SimpleFrontendPusher();
		IKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		WorkflowDefinitionRegistry registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		WorkflowProjection wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry);
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
	}

}
