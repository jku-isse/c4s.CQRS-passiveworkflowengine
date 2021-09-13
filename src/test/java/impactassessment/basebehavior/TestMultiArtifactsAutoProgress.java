package impactassessment.basebehavior;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import org.axonframework.eventhandling.ReplayStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

import artifactapi.ArtifactIdentifier;
import impactassessment.api.Events.*;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.artifactconnector.demo.Basic1Artifacts;
import impactassessment.artifactconnector.demo.DemoRequirement;
import impactassessment.artifactconnector.demo.DemoService;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.WorkflowDefinitionRegistry;
import passiveprocessengine.definition.TaskLifecycle.State;
import passiveprocessengine.instance.WorkflowInstance;

public class TestMultiArtifactsAutoProgress {

	ReplayStatus status = ReplayStatus.REGULAR;
	String workflowId = "TestId1";
	String wfd = "BASICTEST1";
	

	WorkflowProjection wfp;
	ProjectionModel pModel;
	IKieSessionService kieS;
	DemoService ds;
	WorkflowDefinitionRegistry wfdReg;
	
	@Before
	public void setup() {
		System.setProperty("org.jboss.logging.provider", "slf4j");
		Injector inj =  BaseBehaviorTestConfig.getInjector();
		wfp = inj.getInstance(WorkflowProjection.class);
		pModel = inj.getInstance(ProjectionModel.class);
		ds = inj.getInstance(DemoService.class);
		wfdReg = inj.getInstance(WorkflowDefinitionRegistry.class);
		kieS = inj.getInstance(IKieSessionService.class);
	}
	
	/////////////// FIRST RENAME autoprogress.drl.deact back to ...drl /////////////////////////
	
	
	@Test
	public void runPrematureActivateTaskTest() {
		wfp.on(new DeletedEvt(workflowId)); // to ensure any previous workflow is removed
		Basic1Artifacts.initServiceWithReq(ds);
		ArtifactIdentifier ai = Basic1Artifacts.req1.getArtifactIdentifier();
		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(ai, "req"), wfd, wfdReg.get(wfd).getWfd()), status);
		
		
		// prematurely activate "closed"
//		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.assessment.toString(), DemoRequirement.assessmentValues.inpreparation.toString());
//		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req1.getArtifactIdentifier())));
//		wfp.handle(new PrintKBQuery(workflowId));
//		// check that there are now both tasks
//		assertEquals(2, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().size());
//		
		// complete "open"
//		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.inprogress.toString());
//		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req1.getArtifactIdentifier())));
//		// this should trigger completion and cause datamappings, thus activating the regular activation rule, and hence align actual and expected state as "Active"
//		assertEquals(State.ACTIVE, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
//				.filter(task -> task.getType().getId().equals("Closed"))
//				.map(task -> task.getExpectedLifecycleState() )
//				.findFirst().get() );
//		assertEquals(State.ACTIVE, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
//				.filter(task -> task.getType().getId().equals("Closed"))
//				.map(task -> task.getActualLifecycleState() )
//				.findFirst().get() );
//		assertEquals(2, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
//				.filter(task -> task.getType().getId().equals("Closed"))
//				.flatMap(task -> task.getAllInputsByRole("relItems").stream())
//				.count() );
//		
		
		// complete "closed"
//		Basic1Artifacts.req2.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.completed.toString());
//		Basic1Artifacts.req4.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.completed.toString());
//		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req1.getArtifactIdentifier(), Basic1Artifacts.req2.getArtifactIdentifier(), Basic1Artifacts.req4.getArtifactIdentifier())));
//		
		wfp.handle(new PrintKBQuery(workflowId));
		pModel.getWorkflowModel(workflowId).getWorkflowInstance().getAllOutputsByRole("relItems").stream()
		.forEach(art -> System.out.println(art.getArtifactIdentifier()));
		assertEquals(2, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getAllOutputsByRole("relItems").size());
	}
	

}
