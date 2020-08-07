package impactassessment.passiveprocessengine;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.model.WorkflowInstanceWrapper;
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
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#test", rl));
        wfiWrapper.handle(new ActivatedInOutBranchEvt(ID, "open2inProgressOrResolved#test", "Open#test", "resolvedIn"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2inProgressOrResolved#test", rl));

        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();

        // TODO: No data mapping defined in dronology workflow
        // TODO: add assertions when mappings are defined
    }

    @Test
    public void testMapOutputsToExpectedInputsSimpleWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new SimpleWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#"+ID, rl));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, "open2closed#"+ID, "Open#test"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2closed#"+ID, rl));

        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();

        WorkflowTask wftClosed = wfi.getWorkflowTasksReadonly().stream()
                .filter(x -> x.getId().equals("Closed#"+ID))
                .findFirst()
                .get();

        assertEquals(0, wftClosed.getInput().size());
        boolean success = wfi.executeAllMappings();
        assertEquals(true, success);
        assertEquals(1, wftClosed.getInput().size());
    }
}
