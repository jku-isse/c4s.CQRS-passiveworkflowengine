package impactassessment.passiveprocessengine;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.passiveprocessengine.workflowmodel.*;
import impactassessment.passiveprocessengine.workflows.ComplexWorkflow;
import impactassessment.passiveprocessengine.workflows.SimpleWorkflow;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static impactassessment.passiveprocessengine.workflows.ComplexWorkflow.*;
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
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(TD_TASK_OPEN, TD_TASK_CLOSED);

        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_REQ_OPEN+"#"+ID));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_OPEN2CLOSED+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        WorkflowTask wftClosed = wfiWrapper.getWorkflowInstance().getWorkflowTask(TD_TASK_CLOSED+"#"+ID);
        WorkflowTask wftWorking = wfiWrapper.getWorkflowInstance().getWorkflowTask(TD_REQ_WORKING+"#"+ID);
        assertEquals(1, wftClosed.getInput().size());
        assertEquals(0, wftWorking.getInput().size());
        assertEquals(1, dni.getMappingReports().size());
    }

    @Test
    public void testMapOutputsToExpectedInputsComplexWorkflowAdditionalMappingQACheckDocNotPresent() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();

        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(TD_TASK_OPEN, TD_TASK_CLOSED);
        dni.getDefinition().addMapping(TD_REQ_OPEN, TD_REQ_WORKING);

        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_REQ_OPEN+"#"+ID));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_OPEN2CLOSED+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new AddedQAConstraintEvt(ID, TD_TASK_OPEN+"#"+ID, "Status", "RuleName", "Description"));
        wfiWrapper.handle(new AddedQAConstraintEvt(ID, TD_REQ_OPEN+"#"+ID, "Status", "RuleName", "Description"));


        WorkflowTask wftClosed = wfi.getWorkflowTask(TD_TASK_CLOSED+"#"+ID);
        WorkflowTask wftWorking = wfi.getWorkflowTask(TD_REQ_WORKING+"#"+ID);
        assertEquals(1, wftClosed.getInput().size());
        assertEquals(1, wftWorking.getInput().size());
        assertEquals(2, dni.getMappingReports().size());
    }

    @Test
    public void testMapOutputsToExpectedInputsComplexWorkflowAdditionalMappingQACheckDocPresent() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();

        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(TD_TASK_OPEN, TD_TASK_CLOSED);
        dni.getDefinition().addMapping(TD_REQ_OPEN, TD_REQ_WORKING);

        wfiWrapper.handle(new AddedQAConstraintEvt(ID, TD_TASK_OPEN+"#"+ID, "Status", "RuleName", "Description"));
        wfiWrapper.handle(new AddedQAConstraintEvt(ID, TD_REQ_OPEN+"#"+ID, "Status", "RuleName", "Description"));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_REQ_OPEN+"#"+ID));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_OPEN2CLOSED+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        WorkflowTask wftClosed = wfi.getWorkflowTask(TD_TASK_CLOSED+"#"+ID);
        WorkflowTask wftWorking = wfi.getWorkflowTask(TD_REQ_WORKING+"#"+ID);
        assertEquals(2, wftClosed.getInput().size());
        assertEquals(2, wftWorking.getInput().size());
        assertEquals(4, dni.getMappingReports().size());
    }

    @Test
    public void testMapOutputsToExpectedInputsComplexWorkflowAdditionalMappingQACheckDocPresentMappingTypeALL() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();

        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(List.of(TD_TASK_OPEN, TD_REQ_OPEN), List.of(TD_TASK_CLOSED, TD_REQ_WORKING), MappingDefinition.MappingType.ALL);

        wfiWrapper.handle(new AddedQAConstraintEvt(ID, TD_TASK_OPEN+"#"+ID, "Status", "RuleName", "Description"));
        wfiWrapper.handle(new AddedQAConstraintEvt(ID, TD_REQ_OPEN+"#"+ID, "Status", "RuleName", "Description"));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_REQ_OPEN+"#"+ID));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_OPEN2CLOSED+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        WorkflowTask wftClosed = wfi.getWorkflowTask(TD_TASK_CLOSED+"#"+ID);
        WorkflowTask wftWorking = wfi.getWorkflowTask(TD_REQ_WORKING+"#"+ID);
        assertEquals(4, wftClosed.getInput().size());
        assertEquals(4, wftWorking.getInput().size());
        assertEquals(8, dni.getMappingReports().size());
    }
}
