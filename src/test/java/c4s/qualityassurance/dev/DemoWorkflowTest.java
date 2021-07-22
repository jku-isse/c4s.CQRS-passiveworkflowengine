package c4s.qualityassurance.dev;

import java.util.AbstractMap;
import java.util.List;
import java.util.Objects;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.inject.Injector;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.jira.IJiraArtifact;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;
import passiveprocessengine.definition.TaskStateTransitionEventPublisher;
import passiveprocessengine.instance.TaskStateTransitionEvent;

class DemoWorkflowTest {

	ReplayStatus status = ReplayStatus.REGULAR;
	String workflowId = "TestId1";
	WorkflowDefinitionRegistry registry;
	IJiraService jiraS;
	WorkflowProjection wfp;
	ProjectionModel pModel;

	@BeforeEach
	public void setup() {
		Injector injector = DemoConfig.getInjector();
		CommandGateway gw = injector.getInstance(CommandGateway.class);
		IArtifactRegistry aRegistry = new ArtifactRegistry();
		pModel = new ProjectionModel(aRegistry);
		jiraS = DemoConfig.getJiraDemoService();
		aRegistry.register(jiraS);
		IFrontendPusher fp = new SimpleFrontendPusher();
		IKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		wfp = new WorkflowProjection(pModel, kieS, gw, registry, fp, aRegistry);
		((MockCommandGateway) gw).setWorkflowProjection(wfp);

		// access workflow instance
//		pModel.getWorkflowModel(workflowId).getWorkflowInstance()
//				.setTaskStateTransitionEventPublisher(new TaskStateTransitionEventPublisher() {
//
//					@Override
//					public void publishEvent(TaskStateTransitionEvent evt) {
//						System.out.println("[EVENT LISTENER] " + evt.toString());
//					}
//				});
	}

	@Test
	public void testDemoWorkflow() {

		// new AddOutputCmd(id, id, null, "outDoc", new ArtifactType("IJiraArtifact") );

		IJiraArtifact jiraArt = (IJiraArtifact) jiraS
				.get(new ArtifactIdentifier("UAV-1292", "IJiraArtifact"), workflowId).get();

//
		wfp.on(new CreatedWorkflowEvt(workflowId,
				List.of(new AbstractMap.SimpleEntry<>("ROLE_WPTICKET",
						new ArtifactIdentifier("UAV-1292", "IJiraArtifact"))),
				"DRONOLOGY_WORKFLOW", registry.get("DRONOLOGY_WORKFLOW").getWfd()), status);
		// kieS.getKieSession(workflowId).fireAllRules();
		// wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new
		// ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")),
		// status);

		wfp.handle(new PrintKBQuery(workflowId));

		pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
				.map(wft -> wft.getAnyOneOutputByRole("ARTIFACT_TYPE_QA_CHECK_DOCUMENT")).filter(Objects::nonNull)
				.forEach(qa -> System.out.println("QA-DOCUMENT: " + qa));

		pModel.getWorkflowModel(workflowId).getWorkflowInstance()
				.setTaskStateTransitionEventPublisher(new TaskStateTransitionEventPublisher() {

					@Override
					public void publishEvent(TaskStateTransitionEvent evt) {
						System.out.println("[EVENT LISTENER] " + evt.toString());
					}
				});
	}
}
