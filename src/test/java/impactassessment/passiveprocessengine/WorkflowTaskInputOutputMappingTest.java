package impactassessment.passiveprocessengine;

import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.model.workflowmodel.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class WorkflowTaskInputOutputMappingTest {

    private ResourceLink rl;
    private final String ID = "test";

    @Before
    public void setup() {
        IJiraArtifact a = JiraMockService.mockArtifact(ID);
        rl = ResourceLink.of(a);
    }

    /**
     * workflow: OPEN --> CLOSED
     * OPEN output: ResourceLink
     * CLOSED expected input: ResourceLink
     */
    @Test
    public void testMapOutputsToExpectedInputsSameRole() {
        // create a new workflow definition
        TestWorkflow workflow = new TestWorkflow();
        workflow.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // publisher must be set to prevent NullPointer

        // create an instance out of the workflow definition
        WorkflowInstance wfi = workflow.createInstance(ID);
        wfi.enableWorkflowTasksAndDecisionNodes();

        // get first decision node (kickoff)
        DecisionNodeInstance dniKickoff = wfi.getDecisionNodeInstancesReadonly().stream()
                .filter(x -> x.getId().equals("workflowKickOff#"+ID))
                .findFirst()
                .get();
        dniKickoff.completedDataflowInvolvingActivationPropagation();

        // get tasks from first decision node and complete dataflow
        List<TaskDefinition> taskDefinitionsOpen = dniKickoff.getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks();
        taskDefinitionsOpen.stream()
                .forEach(td -> {
                    WorkflowTask wft = wfi.instantiateTask(td);
                    wft.addOutput(new WorkflowTask.ArtifactOutput(rl, TestWorkflow.ROLE_WPTICKET));
                    wft.signalEvent(TaskLifecycle.Events.INPUTCONDITIONS_FULFILLED);
                    wfi.activateDecisionNodesFromTask(wft);
                    dniKickoff.consumeTaskForUnconnectedOutBranch(wft);
                });

        // get second (and last) decision node (open2closed)
        DecisionNodeInstance dniOpen2Closed = wfi.getDecisionNodeInstancesReadonly().stream()
                .filter(x -> x.getId().equals("open2closed#"+ID))
                .findFirst()
                .get();
        dniOpen2Closed.activateInBranch("openOut");
        dniOpen2Closed.completedDataflowInvolvingActivationPropagation();


        // get tasks from first decision node and complete dataflow
        List<TaskDefinition> taskDefinitionsClosed = dniOpen2Closed.getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks();
        taskDefinitionsClosed.stream()
                .forEach(td -> {
                    WorkflowTask wft = wfi.instantiateTask(td);
                    wft.signalEvent(TaskLifecycle.Events.INPUTCONDITIONS_FULFILLED);
                    wfi.activateDecisionNodesFromTask(wft);
                    dniOpen2Closed.consumeTaskForUnconnectedOutBranch(wft);
                });

        WorkflowTask wftClosed = wfi.getWorkflowTasksReadonly().stream()
                .filter(x -> x.getId().equals("Closed#"+ID))
                .findFirst()
                .get();

        // before the mapping workflow task "Closed" shouldn't have inputs
        assertEquals(0, wftClosed.getInput().size());

        boolean success = dniOpen2Closed.mapOutputsToExpectedInputsSameRole();
//        boolean success = wfi.mapOutputsToExpectedInputsSameRole(); // trigger mapping for all DNIs

        // after the mapping workflow task "Closed" has one input
        assertEquals(true, success);
        assertEquals(1, wftClosed.getInput().size());
    }
}
