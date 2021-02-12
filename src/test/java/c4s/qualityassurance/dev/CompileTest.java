package c4s.qualityassurance.dev;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jira.IJiraArtifact;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.command.MockCommandGateway;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;
import com.google.inject.Injector;

import impactassessment.DevelopmentConfig;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;
import passiveprocessengine.instance.ResourceLink;

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
		ReplayStatus status = ReplayStatus.REGULAR;
		String workflowId = "TestId1";
		//new AddOutputCmd(id, id, null, "outDoc", new ArtifactType("IJiraArtifact") );
		
		
	}

}
