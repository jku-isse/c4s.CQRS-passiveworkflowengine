package workflowtests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import artifactapi.ArtifactIdentifier;
import artifactapi.ResourceLink;
import impactassessment.ltlcheck.LTLFormulaProvider.AvailableFormulas;
import impactassessment.ltlcheck.LTLValidationManager;
import impactassessment.ltlcheck.util.ValidationUtil.ValidationSelection;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.definition.TaskLifecycle.State;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.QACheckDocument;
import passiveprocessengine.instance.QACheckDocument.QAConstraint;
import passiveprocessengine.instance.WorkflowInstance;
import workflowtests.ComplexXorWorkflow.TASKS;

public class XorTests {

	private ResourceLink rl;
	private final String ID = "test";
	private QACheckDocument qaDoc;
	private QAConstraint qac;
	private ResourceLink rl2;

	@Before
	public void setup() {
		rl = new ResourceLink("context", "href", "rel", "as", "linkType", "title");
		rl2 = new ResourceLink("context2", "href2", "rel2", "as2", "linkType2", "title2");
		qac = new QAConstraint() {
			boolean ok = false;

			@Override
			public boolean isFulfilled() {
				return ok;
			}

			@Override
			public void checkConstraint() {
				ok = true;
			}

			@Override
			public ArtifactIdentifier getArtifactIdentifier() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	@Test
	public void testSingleXor() {
		// create a new workflow definition
		ComplexXorWorkflow workflow = new ComplexXorWorkflow();
		workflow.setTaskStateTransitionEventPublisher(event -> {
			System.out.println(event);
		}); // publisher must be set to prevent NullPointer
		// create an instance out of the workflow definition
		WorkflowInstance wfi = workflow.createInstance(ID);
		qaDoc = new QACheckDocument("someid", wfi);
		qaDoc.addConstraint(qac);
		wfi.addInput(new ArtifactInput(rl, "INPUT_ROLE_WPTICKET"));
		wfi.enableWorkflowTasksAndDecisionNodes();

		wfi.getWorkflowTask(TASKS.ImpactAssessment + "#" + ID).postConditionsFulfilled();
		wfi.getWorkflowTask(TASKS.Specifying + "#" + ID).postConditionsFulfilled();

		IWorkflowTask modeling = wfi.getWorkflowTask(TASKS.Modeling + "#" + ID);
		IWorkflowTask designing = wfi.getWorkflowTask(TASKS.Designing + "#" + ID);
		// choose one of the xor branches
		designing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_SPEC"));
		designing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_SSDDREVIEW"));
		designing.postConditionsFulfilled();
		// assert all other tasks are in no work expected state
		assertEquals(State.NO_WORK_EXPECTED, modeling.getExpectedLifecycleState());

		modeling.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_MODEL"));
		IWorkflowTask proofing = wfi.getWorkflowTask(TASKS.Proofing + "#" + ID);
		proofing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_PROOF"));
		// this is a deviation, as the other branch is already chosen
		assertEquals(State.NO_WORK_EXPECTED, modeling.getExpectedLifecycleState());
		assertEquals(State.NO_WORK_EXPECTED, proofing.getExpectedLifecycleState());
		assertEquals(State.COMPLETED, modeling.getActualLifecycleState());
		assertEquals(State.COMPLETED, proofing.getActualLifecycleState());

		// now continue further below:
		IWorkflowTask coding = wfi.getWorkflowTask(TASKS.Coding + "#" + ID);
		coding.postConditionsFulfilled();
		IWorkflowTask manTesting = wfi.getWorkflowTask(TASKS.ManualTesting + "#" + ID);
		IWorkflowTask autoTesting = wfi.getWorkflowTask(TASKS.AutomatedTesting + "#" + ID);
		IWorkflowTask noTesting = wfi.getWorkflowTask(TASKS.NoTesting + "#" + ID);
		autoTesting.activate();
		manTesting.setCanceled(true);
		noTesting.activate();
		autoTesting.postConditionsFulfilled();
		assertEquals(State.COMPLETED, autoTesting.getExpectedLifecycleState());
		assertEquals(State.NO_WORK_EXPECTED, noTesting.getExpectedLifecycleState());
		assertEquals(State.CANCELED, manTesting.getExpectedLifecycleState());
		assertEquals(State.COMPLETED, autoTesting.getActualLifecycleState());
		assertEquals(State.NO_WORK_EXPECTED, noTesting.getActualLifecycleState());
		assertEquals(State.CANCELED, manTesting.getActualLifecycleState());

		manTesting.activate();
		assertEquals(State.CANCELED, manTesting.getExpectedLifecycleState()); // only when we reset() which is not
																				// publicly exposed yet, should it be
																				// expected no work expected, which
																				// again is not implemented yed
		assertEquals(State.ACTIVE, manTesting.getActualLifecycleState());
	}

	@Test
	public void testSingleXorOverride() {
		// create a new workflow definition
		ComplexXorWorkflow workflow = new ComplexXorWorkflow();
		workflow.setTaskStateTransitionEventPublisher(event -> {
			System.out.println(event);
		}); // publisher must be set to prevent NullPointer
		// create an instance out of the workflow definition
		WorkflowInstance wfi = workflow.createInstance(ID);
		qaDoc = new QACheckDocument("someid", wfi);
		qaDoc.addConstraint(qac);
		wfi.addInput(new ArtifactInput(rl, "INPUT_ROLE_WPTICKET"));
		wfi.enableWorkflowTasksAndDecisionNodes();

		wfi.getWorkflowTask(TASKS.ImpactAssessment + "#" + ID).postConditionsFulfilled();
		wfi.getWorkflowTask(TASKS.Specifying + "#" + ID).postConditionsFulfilled();

		IWorkflowTask modeling = wfi.getWorkflowTask(TASKS.Modeling + "#" + ID);
		IWorkflowTask designing = wfi.getWorkflowTask(TASKS.Designing + "#" + ID);
		// choose one of the xor branches
		designing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_SPEC"));
		designing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_SSDDREVIEW"));
		designing.postConditionsFulfilled();
		// assert all other tasks are in no work expected state
		assertEquals(State.NO_WORK_EXPECTED, modeling.getExpectedLifecycleState());

		// now we deviate by continuing in the non-selected branch
		modeling.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_MODEL"));
		IWorkflowTask proofing = wfi.getWorkflowTask(TASKS.Proofing + "#" + ID);
		proofing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_PROOF"));

		assertEquals(State.NO_WORK_EXPECTED, modeling.getExpectedLifecycleState());
		assertEquals(State.NO_WORK_EXPECTED, proofing.getExpectedLifecycleState());
		assertEquals(State.COMPLETED, modeling.getActualLifecycleState());
		assertEquals(State.COMPLETED, proofing.getActualLifecycleState());

		// now we set original branch to CANCLED (to signal we dont want to use it)
		designing.setCanceled(true);
		assertEquals(State.CANCELED, designing.getExpectedLifecycleState());
		assertEquals(State.CANCELED, designing.getActualLifecycleState());

		// TODO: now we should activate the other branch as it can be used now --> needs
		// propagation of status change to XOR
		// TODO: undo any data mapping

		// TODO: if a reactivation/reset of the canceled branch occur, then the other
		// should be no work expected again

		wfi.getWorkflowTasksReadonly().stream().forEach(wft -> System.out.println(wft.toString()));
		wfi.getDecisionNodeInstancesReadonly().stream().forEach(wft -> System.out.println(wft.toString()));

	}

	@Test
	public void testStartOneXorBranchFinishOther() {
		// create a new workflow definition
		ComplexXorWorkflow workflow = new ComplexXorWorkflow();
		workflow.setTaskStateTransitionEventPublisher(event -> {
			System.out.println(event);
			LTLValidationManager.getInstance().validate(ID, event.getTask().getWorkflow(),
					AvailableFormulas.OUTPUT_MISSING, ValidationSelection.SPECIAL, true);
			// LTLValidationManager.getInstance().clearResults(ID);
		}); // publisher must be set to prevent NullPointer
		// create an instance out of the workflow definition
		WorkflowInstance wfi = workflow.createInstance(ID);
		qaDoc = new QACheckDocument("someid", wfi);
		qaDoc.addConstraint(qac);
		wfi.addInput(new ArtifactInput(rl, "INPUT_ROLE_WPTICKET"));
		wfi.enableWorkflowTasksAndDecisionNodes();

		wfi.getWorkflowTask(TASKS.ImpactAssessment + "#" + ID).postConditionsFulfilled();
		wfi.getWorkflowTask(TASKS.Specifying + "#" + ID).postConditionsFulfilled();

		IWorkflowTask modeling = wfi.getWorkflowTask(TASKS.Modeling + "#" + ID);
		IWorkflowTask designing = wfi.getWorkflowTask(TASKS.Designing + "#" + ID);
		// choose one of the xor branches
		modeling.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_MODEL"));
		IWorkflowTask proofing = wfi.getWorkflowTask(TASKS.Proofing + "#" + ID);

		// now we complete the other:
		designing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_SPEC"));
		designing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_SSDDREVIEW"));
		designing.postConditionsFulfilled();

		wfi.getWorkflowTasksReadonly().stream().forEach(wft -> System.out.println(wft.toString()));
		wfi.getDecisionNodeInstancesReadonly().stream().forEach(wft -> System.out.println(wft.toString()));

		// assert all other tasks are in no work expected state
		assertEquals(State.NO_WORK_EXPECTED, modeling.getActualLifecycleState());
		assertEquals(State.NO_WORK_EXPECTED, proofing.getExpectedLifecycleState());

		// now we explicitly cancel the step:
		modeling.setCanceled(true);
		assertEquals(State.CANCELED, modeling.getExpectedLifecycleState());

		// and again switch back to the other
		proofing.addOutput(new ArtifactOutput(rl, "OUTPUT_ROLE_PROOF"));
		// TODO: this should indicate that the original branch should be deactivated,
		// and these ones here activated

		wfi.getWorkflowTasksReadonly().stream().forEach(wft -> System.out.println(wft.toString()));
		wfi.getDecisionNodeInstancesReadonly().stream().forEach(wft -> System.out.println(wft.toString()));

//        IWorkflowTask openT = wfi.getWorkflowTask(ParallelNoCondWorkflowWithDatamapping.TASK_STATE_OPEN+"#test");
//        openT.addOutput(new ArtifactOutput(qaDoc, ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
//        // Get First Para Step and signal completion
//        IWorkflowTask docT = wfi.getWorkflowTask(ParallelNoCondWorkflowWithDatamapping.TASK_STATE_DOC+"#test");
//        docT.addOutput(new ArtifactOutput(rl, ParallelNoCondWorkflowWithDatamapping.ROLE_DOC));
//        qac.checkConstraint(); //switches this constraint to fulfilled
//        openT.postConditionsFulfilled();
//
//        // now we should have Report input fullfilled and closed input fullfilled, started due to OR SYNC
//        IWorkflowTask closedT = wfi.getWorkflowTask(ParallelNoCondWorkflowWithDatamapping.TASK_STATE_CLOSED+"#test");
//        IWorkflowTask reportT = wfi.getWorkflowTask(ParallelNoCondWorkflowWithDatamapping.TASK_STATE_REPORTED+"#test");
//
//        assertEquals(State.ENABLED, closedT.getActualLifecycleState());
//        assertEquals(State.ENABLED, reportT.getActualLifecycleState());
//        assertEquals(State.COMPLETED, openT.getActualLifecycleState());
//        assertEquals(State.COMPLETED, docT.getActualLifecycleState());
//        assertEquals(1, reportT.getAllInputsByRole(ParallelNoCondWorkflowWithDatamapping.ROLE_DOC).size());
//
//        // now lets start (and actually finish) the report Task:
//        reportT.addOutput(new ArtifactOutput(rl, ParallelNoCondWorkflowWithDatamapping.ROLE_DOC));
//
//        // now lets add additional input to docT:
//        docT.getOutput().stream().filter(ao -> ao.getRole().equals(ParallelNoCondWorkflowWithDatamapping.ROLE_DOC)).findAny().ifPresent(ao -> ao.addOrReplaceArtifact(rl2));
//        // as there is no other unfullfilled condition to complete (neither postCond, nor QA), we immediately transition from ACTIVE back into COMPLETED
//        assertEquals(State.COMPLETED, docT.getActualLifecycleState());
//        // now lets complete that task again and check if the additional output is mapped
//        assertEquals(2, reportT.getAllInputsByRole(ParallelNoCondWorkflowWithDatamapping.ROLE_DOC).size());
//
//
		// REQUIRES DIFFERENT PROCESS
		// now lets test this with an explicit ACTIVE state
		// openT.getOutput().stream().filter(ao ->
		// ao.getRole().equals(ParallelNoCondWorkflowWithDatamapping.ROLE_TASK)).findAny().ifPresent(ao
		// -> ao.addOrReplaceArtifact(rl2));
		// as there is no other unfullfilled condition to complete (neither postCond,
		// nor QA), we immediately transition from ACTIVE back into COMPLETED
		// assertEquals(State.COMPLETED, docT.getActualLifecycleState());

	}
}