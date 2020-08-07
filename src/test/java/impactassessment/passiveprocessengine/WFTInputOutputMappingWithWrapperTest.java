package impactassessment.passiveprocessengine;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.model.workflowmodel.DecisionNodeDefinition;
import impactassessment.model.workflowmodel.ResourceLink;
import impactassessment.model.workflowmodel.WorkflowInstance;
import impactassessment.model.workflowmodel.WorkflowTask;
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

    @Test
    public void testMapOutputsToExpectedInputsDronologyWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new ImportedOrUpdatedArtifactEvt(ID, a));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInOutBranchEvt(ID, "open2inProgressOrResolved#test", "Open#test", "resolvedIn"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2inProgressOrResolved#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();

        // TODO: No data mapping defined in dronology workflow
        // TODO: add assertions when mappings are defined
    }

    @Test
    public void testMapOutputsToExpectedInputsSimpleWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new SimpleWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, "open2closed#"+ID, "Open#test"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2closed#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();

        WorkflowTask wftClosed = wfi.getWorkflowTasksReadonly().stream()
                .filter(x -> x.getId().equals("Closed#"+ID))
                .findFirst()
                .get();

        assertEquals(0, wftClosed.getInput().size());
        int numMappings = wfi.executeAllMappings();
        assertEquals(1, numMappings);
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

        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();

        WorkflowTask wftClosed = wfi.getWorkflowTasksReadonly().stream()
                .filter(x -> x.getId().equals(ComplexWorkflow.TD_TASK_CLOSED+"#"+ID))
                .findFirst()
                .get();

        assertEquals(0, wftClosed.getInput().size());
        int numMappings = wfi.executeAllMappings();
        assertEquals(1, numMappings);
        assertEquals(1, wftClosed.getInput().size());
    }

    @Test
    public void testMapOutputsToExpectedInputsComplexWorkflowAdditionalMappings() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, ComplexWorkflow.DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, ComplexWorkflow.TD_REQ_OPEN+"#"+ID));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, ComplexWorkflow.DND_OPEN2CLOSED+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        DecisionNodeDefinition dndOpen2Closed = wfiWrapper.getWorkflowInstance().getWorkflowDefinition().getDNDbyID(ComplexWorkflow.DND_OPEN2CLOSED);
        dndOpen2Closed.addMapping(ComplexWorkflow.TD_REQ_OPEN, ComplexWorkflow.TD_REQ_WORKING);

        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();

        WorkflowTask wftClosed = wfi.getWorkflowTasksReadonly().stream()
                .filter(x -> x.getId().equals(ComplexWorkflow.TD_TASK_CLOSED+"#"+ID))
                .findFirst()
                .get();

        assertEquals(0, wftClosed.getInput().size());
        int numMappings = wfi.executeAllMappings();
        assertEquals(2, numMappings);
        assertEquals(1, wftClosed.getInput().size());
    }
}
