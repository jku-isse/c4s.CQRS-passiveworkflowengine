package c4s.qualityassurance.dev;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.jama.IJamaArtifact;
import com.google.inject.Injector;
import impactassessment.DevelopmentConfig;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.IJiraService;
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
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;

import java.util.Map;

class UserStudyWorkflowTest {

	

	public static void main(String[] args) {
		IJiraService jiraS = UserStudyJiraConfig.getJiraService(false);	
		Injector injector = UserStudyJiraConfig.getInjector();
		IArtifactRegistry aRegistry = injector.getInstance(IArtifactRegistry.class);		
		aRegistry.register(jiraS);

		WorkflowDefinitionRegistry registry = injector.getInstance(WorkflowDefinitionRegistry.class);
		WorkflowProjection wfp = injector.getInstance(WorkflowProjection.class);
		ReplayStatus status = ReplayStatus.REGULAR;
		String workflowId = "TestId1";
		
		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(new ArtifactIdentifier("SIELA-20", "IJiraArtifact"), "story"), "SIELA", registry.get("SIELA").getWfd()), status);		
		wfp.handle(new PrintKBQuery(workflowId));

	}

}
