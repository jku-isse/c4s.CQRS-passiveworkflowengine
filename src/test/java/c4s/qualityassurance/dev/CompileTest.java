package c4s.qualityassurance.dev;

import artifactapi.IArtifactRegistry;

import org.axonframework.commandhandling.gateway.CommandGateway;

import com.google.inject.Injector;
import impactassessment.DevelopmentConfig;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.command.DefaultGatewayProxyFactory;
import impactassessment.command.IGatewayProxyFactory;
import impactassessment.command.MockCommandGateway;

import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.EventList2Forwarder;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

class CompileTest {

	

	public static void main(String[] args) {
		Injector injector = DevelopmentConfig.getInjector();
		CommandGateway gw = injector.getInstance(CommandGateway.class);
		IArtifactRegistry aRegistry = new ArtifactRegistry();
		ProjectionModel pModel = new ProjectionModel(aRegistry);
		IFrontendPusher fp = new SimpleFrontendPusher();
		IGatewayProxyFactory gpf = new DefaultGatewayProxyFactory(gw);
		IKieSessionService kieS = new SimpleKieSessionService(aRegistry, gpf);
		WorkflowDefinitionRegistry registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		WorkflowProjection wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry, new EventList2Forwarder());
		((MockCommandGateway) gw).setWorkflowProjection(wfp);
	}

}
