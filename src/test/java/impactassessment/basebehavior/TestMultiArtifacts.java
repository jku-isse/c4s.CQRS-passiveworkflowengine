package impactassessment.basebehavior;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.axonframework.eventhandling.ReplayStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
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

public class TestMultiArtifacts {

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
	
/////////////// FIRST ENSURE autoprogress.drl is not active by adding .deact as filepostfix /////////////////////////
	
	@Test
	public void runMultiArtifactOutputTest() {
		wfp.on(new DeletedEvt(workflowId)); // to ensure any previous workflow is removed
		Basic1Artifacts.initServiceWithReq(ds);
		ArtifactIdentifier ai = Basic1Artifacts.req1.getArtifactIdentifier();

		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(ai, "req"), wfd, wfdReg.get(wfd).getWfd()), status);
		wfp.handle(new PrintKBQuery(workflowId));

		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.inprogress.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(ai)));
		
		Basic1Artifacts.req2.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.completed.toString());
		Basic1Artifacts.req4.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.completed.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req2.getArtifactIdentifier(), Basic1Artifacts.req4.getArtifactIdentifier())));

		wfp.handle(new PrintKBQuery(workflowId));
		pModel.getWorkflowModel(workflowId).getWorkflowInstance().getAllOutputsByRole("relItems").stream()
		.forEach(art -> System.out.println(art.getArtifactIdentifier()));
		assertEquals(2, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getAllOutputsByRole("relItems").size());
	}

	@Test
	public void runPrematureActivateTaskTest() {
		wfp.on(new DeletedEvt(workflowId)); // to ensure any previous workflow is removed
		Basic1Artifacts.initServiceWithReq(ds);
		ArtifactIdentifier ai = Basic1Artifacts.req1.getArtifactIdentifier();
		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(ai, "req"), wfd, wfdReg.get(wfd).getWfd()), status);
		
		
		// prematurely activate "closed"
		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.assessment.toString(), DemoRequirement.assessmentValues.inpreparation.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req1.getArtifactIdentifier())));
		wfp.handle(new PrintKBQuery(workflowId));
		// check that there are now both tasks
		assertEquals(2, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().size());
		
		// complete "open"
		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.inprogress.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req1.getArtifactIdentifier())));
		// this should trigger completion and cause datamappings, thus activating the regular activation rule, and hence align actual and expected state as "Active"
		assertEquals(State.ACTIVE, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
				.filter(task -> task.getType().getId().equals("Closed"))
				.map(task -> task.getExpectedLifecycleState() )
				.findFirst().get() );
		assertEquals(State.ACTIVE, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
				.filter(task -> task.getType().getId().equals("Closed"))
				.map(task -> task.getActualLifecycleState() )
				.findFirst().get() );
		assertEquals(2, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
				.filter(task -> task.getType().getId().equals("Closed"))
				.flatMap(task -> task.getAllInputsByRole("relItems").stream())
				.count() );
		
		
		// complete "closed"
		Basic1Artifacts.req2.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.completed.toString());
		Basic1Artifacts.req4.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.completed.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req1.getArtifactIdentifier(), Basic1Artifacts.req2.getArtifactIdentifier(), Basic1Artifacts.req4.getArtifactIdentifier())));
		
		wfp.handle(new PrintKBQuery(workflowId));
		pModel.getWorkflowModel(workflowId).getWorkflowInstance().getAllOutputsByRole("relItems").stream()
		.forEach(art -> System.out.println(art.getArtifactIdentifier()));
		assertEquals(2, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getAllOutputsByRole("relItems").size());
	}
	
	@Test
	public void runPrematureCompleteTaskTest() {
		wfp.on(new DeletedEvt(workflowId)); // to ensure any previous workflow is removed
		Basic1Artifacts.initServiceWithReq(ds);
		ArtifactIdentifier ai = Basic1Artifacts.req1.getArtifactIdentifier();
		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(ai, "req"), wfd, wfdReg.get(wfd).getWfd()), status);
		WorkflowInstance wfi = pModel.getWorkflowModel(workflowId).getWorkflowInstance();
		
		// prematurely activate "closed"
		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.assessment.toString(), DemoRequirement.assessmentValues.inpreparation.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req1.getArtifactIdentifier())));
		wfp.handle(new PrintKBQuery(workflowId));
		// check that there are now both tasks
		assertEquals(2, pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().size());
		
		// complete "closed"
		Basic1Artifacts.req2.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.completed.toString());
		Basic1Artifacts.req4.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.completed.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req2.getArtifactIdentifier(), Basic1Artifacts.req4.getArtifactIdentifier())));
		assertEquals(State.AVAILABLE, wfi.getWorkflowTasksReadonly().stream()
				.filter(task -> task.getType().getId().equals("Closed"))
				.map(task -> task.getExpectedLifecycleState() )
				.findFirst().get() );
		// task is not complete yet as the outputs are not ready yet, if that were the case, then the task would go into completed
		assertEquals(State.ACTIVE, wfi.getWorkflowTasksReadonly().stream()
				.filter(task -> task.getType().getId().equals("Closed"))
				.map(task -> task.getActualLifecycleState() )
				.findFirst().get() );		
		
		// complete "open"
		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.inprogress.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(Basic1Artifacts.req1.getArtifactIdentifier())));
		// this should trigger completion of 'open' and cause datamappings, thus activating the regular activation rule, and hence align actual and expected state as "Complete" (as other task post conditions are already true")
		assertEquals(State.COMPLETED, wfi.getWorkflowTasksReadonly().stream() 
				.filter(task -> task.getType().getId().equals("Closed"))
				.map(task -> task.getExpectedLifecycleState() )
				.findFirst().get() );
//		assertEquals(State.COMPLETED, wfi.getWorkflowTasksReadonly().stream() //TODO: will be correct upon change propagation
//				.filter(task -> task.getType().getId().equals("Closed"))
//				.map(task -> task.getActualLifecycleState() )
//				.findFirst().get() );
		assertEquals(2, wfi.getWorkflowTasksReadonly().stream()
				.filter(task -> task.getType().getId().equals("Closed"))
				.flatMap(task -> task.getAllInputsByRole("relItems").stream())
				.count() );
		
		wfp.handle(new PrintKBQuery(workflowId));
		wfi.getAllOutputsByRole("relItems").stream()
		.forEach(art -> System.out.println(art.getArtifactIdentifier()));
		assertEquals(2, wfi.getAllOutputsByRole("relItems").size());
	}
	
	
	@Test
	public void runSpawnTest() {
		wfp.on(new DeletedEvt(workflowId)); // to ensure any previous workflow is removed
		Basic1Artifacts.initServiceWithReq(ds);
		ArtifactIdentifier ai = Basic1Artifacts.req1.getArtifactIdentifier();
		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.type.toString(), DemoRequirement.typeValues.highlevel.toString());

		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(ai, "req"), wfd, wfdReg.get(wfd).getWfd()), status);
		wfp.handle(new PrintKBQuery(workflowId));
		
		// we should now have 2 additional spawned processes: for req2 and req4, note req2 should not spawn another as its of type lowlevel
		assertNotNull(pModel.getWorkflowModel(Basic1Artifacts.req2.getArtifactIdentifier().getId()));
		assertNotNull(pModel.getWorkflowModel(Basic1Artifacts.req4.getArtifactIdentifier().getId()));
		
		// now we change req2 and see if it spawns process for req3:
		Basic1Artifacts.req2.getPropertyMap().put(DemoRequirement.propKeys.type.toString(), DemoRequirement.typeValues.highlevel.toString());
		wfp.on(new UpdatedArtifactsEvt(Basic1Artifacts.req2.getArtifactIdentifier().getId(), List.of(Basic1Artifacts.req2.getArtifactIdentifier())));
		
		assertNotNull(pModel.getWorkflowModel(Basic1Artifacts.req3.getArtifactIdentifier().getId()));
	}
	
	@Test
	public void runUndoTest() {
		wfp.on(new DeletedEvt(workflowId)); // to ensure any previous workflow is removed
		Basic1Artifacts.initServiceWithReq(ds);
		ArtifactIdentifier ai = Basic1Artifacts.req1.getArtifactIdentifier();

		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(ai, "req"), wfd, wfdReg.get(wfd).getWfd()), status);
		wfp.handle(new PrintKBQuery(workflowId));

		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.inprogress.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(ai)));		
		assertEquals(State.COMPLETED, getExpectedState("Open") );
		assertEquals(2, getAllInputArtifactsFromTaskForRole("Closed", "relItems").size());
		
		
		Basic1Artifacts.req1.getPropertyMap().put(DemoRequirement.propKeys.status.toString(), DemoRequirement.statusValues.open.toString());
		wfp.on(new UpdatedArtifactsEvt(workflowId, List.of(ai)));
		assertEquals(State.COMPLETED, getExpectedState("Open") );
		assertEquals(State.ACTIVE, getActualState("Open") );				
		wfp.handle(new PrintKBQuery(workflowId));
		
		wfp.on(new RemovedInputEvt(workflowId, "Open#"+workflowId, ai, "req"), status);
		assertEquals(0, getAllOutputArtifactsFromTaskForRole("Open", "relItems").size()); // because rules remove these, thats not automatic
		
		wfp.handle(new PrintKBQuery(workflowId));
		assertEquals(State.ACTIVE, getExpectedState("Closed") );
		assertEquals(State.AVAILABLE, getActualState("Closed") );
		assertEquals(0, getAllInputArtifactsFromTaskForRole("Closed", "relItems").size());
		
	}

	private State getExpectedState(String taskType) {
		return pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
		.filter(task -> task.getType().getId().equals(taskType))
		.map(task -> task.getExpectedLifecycleState() )
		.findFirst().get();
	}
	
	private State getActualState(String taskType) {
		return pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
		.filter(task -> task.getType().getId().equals(taskType))
		.map(task -> task.getActualLifecycleState() )
		.findFirst().get();
	}
	
	private List<IArtifact> getAllOutputArtifactsFromTaskForRole(String taskType, String role) {
		return pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
				.filter(task -> task.getType().getId().equals(taskType))
				.flatMap(task -> task.getAllOutputsByRole(role).stream())
				.collect(Collectors.toList());
	}
	
	private List<IArtifact> getAllInputArtifactsFromTaskForRole(String taskType, String role) {
		return pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
				.filter(task -> task.getType().getId().equals(taskType))
				.flatMap(task -> task.getAllInputsByRole(role).stream())
				.collect(Collectors.toList());
	}
}
