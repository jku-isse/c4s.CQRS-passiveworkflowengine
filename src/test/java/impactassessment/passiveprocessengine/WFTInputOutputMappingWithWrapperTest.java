package impactassessment.passiveprocessengine;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.passiveprocessengine.workflowmodel.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WFTInputOutputMappingWithWrapperTest {

    private final String ID = "test";
    private IJiraArtifact a;
    private ResourceLink rl;

    @Before
    public void setup() {
        a = JiraMockService.mockArtifact(ID);
        rl = ResourceLink.of(a);
    }

//    @Test
//    public void testMapOutputsToExpectedInputsDronologyWorkflow() {
//        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
//        wfiWrapper.handle(new ImportedOrUpdatedArtifactEvt(ID, a));
//        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
//        wfiWrapper.handle(new ActivatedInOutBranchEvt(ID, "open2inProgressOrResolved#test", "Open#test", "resolvedIn"));
//        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2inProgressOrResolved#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
//
//        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
//
//        // TODO: No data mapping defined in dronology workflow
//        // TODO: add assertions when mappings are defined
//    }

    @Test
    public void testMapOutputsToExpectedInputsSimpleWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new SimpleWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, "open2closed#"+ID, "Open#test"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2closed#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        WorkflowTask wftClosed = wfiWrapper.getWorkflowInstance().getWorkflowTask("Closed#"+ID);
        assertEquals(1, wftClosed.getInput().size());
    }

    @Test
    public void testMapOutputsToExpectedInputsComplexWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, ComplexWorkflow.DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_REQ_OPEN+"#"+ID));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        WorkflowTask wftClosed = wfiWrapper.getWorkflowInstance().getWorkflowTask(ComplexWorkflow.TD_TASK_CLOSED+"#"+ID);
        WorkflowTask wftWorking = wfiWrapper.getWorkflowInstance().getWorkflowTask(ComplexWorkflow.TD_REQ_WORKING+"#"+ID);
        assertEquals(1, wftClosed.getInput().size());
        assertEquals(0, wftWorking.getInput().size());
    }

    @Test
    public void testMapOutputsToExpectedInputsComplexWorkflowAdditionalMappings() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();

        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, ComplexWorkflow.DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(ComplexWorkflow.TD_REQ_OPEN, ComplexWorkflow.TD_REQ_WORKING);

        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_REQ_OPEN+"#"+ID));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new AddedQAConstraintEvt(ID, ComplexWorkflow.TD_TASK_OPEN+"#"+ID, "Status", "RuleName", "Description"));
        wfiWrapper.handle(new AddedQAConstraintEvt(ID, ComplexWorkflow.TD_REQ_OPEN+"#"+ID, "Status", "RuleName", "Description"));


        WorkflowTask wftClosed = wfi.getWorkflowTask(ComplexWorkflow.TD_TASK_CLOSED+"#"+ID);
        WorkflowTask wftWorking = wfi.getWorkflowTask(ComplexWorkflow.TD_REQ_WORKING+"#"+ID);
        assertEquals(1, wftClosed.getInput().size());
        assertEquals(1, wftWorking.getInput().size());
    }
}
