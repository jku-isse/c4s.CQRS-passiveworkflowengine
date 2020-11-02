package impactassessment.passiveprocessengine;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.passiveprocessengine.definition.ArtifactType;
import impactassessment.passiveprocessengine.definition.ArtifactTypes;
import impactassessment.passiveprocessengine.definition.MappingDefinition;
import impactassessment.passiveprocessengine.instance.DecisionNodeInstance;
import impactassessment.passiveprocessengine.instance.ResourceLink;
import impactassessment.passiveprocessengine.instance.WorkflowInstance;
import impactassessment.passiveprocessengine.instance.WorkflowTask;
import impactassessment.passiveprocessengine.workflows.ComplexWorkflow;
import impactassessment.passiveprocessengine.workflows.DronologyWorkflowFixed;
import impactassessment.passiveprocessengine.workflows.SimpleWorkflow;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

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

    @Test
    public void testMapOutputsToExpectedInputsDronologyWorkflowFixed() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new CreatedWorkflowEvt(ID, List.of(a), "", new DronologyWorkflowFixed()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInOutBranchEvt(ID, "open2inProgressOrResolved#test", "Open#test", "inProgressIn"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2inProgressOrResolved#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, "inProgress2resolved#test", "In Progress#test"));
        wfiWrapper.handle(new ActivatedOutBranchEvt(ID, "inProgress2resolved#test", "resolvedIn2"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "inProgress2resolved#test", rl));

        System.out.println("x");
    }

    @Test
    public void testMapOutputsToExpectedInputsSimpleWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new CreatedWorkflowEvt(ID, List.of(a), "", new SimpleWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInOutBranchEvt(ID, "open2closed#"+ID, "Open#test", "openOut"));
        wfiWrapper.handle(new AddedOutputEvt(ID, "Open#test", new ResourceLink("test", "test", "test", "test", "test", "test"), ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT)));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2closed#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        WorkflowTask wftClosed = wfiWrapper.getWorkflowInstance().getWorkflowTask("Closed#"+ID);
        assertEquals(1, wftClosed.getInput().size());
    }

    @Test
    public void testMapOutputsToExpectedInputsComplexWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new CreatedWorkflowEvt(ID, List.of(a), "", new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_REQ_OPEN+"#"+ID));

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(TD_TASK_OPEN, ROLE_QA_CHECK_DOC, TD_TASK_CLOSED, ROLE_QA_CHECK_DOC);

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

        wfiWrapper.handle(new CreatedWorkflowEvt(ID, List.of(a), "", new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(TD_TASK_OPEN, ROLE_QA_CHECK_DOC, TD_TASK_CLOSED, ROLE_QA_CHECK_DOC);
        dni.getDefinition().addMapping(TD_REQ_OPEN, ROLE_QA_CHECK_DOC, TD_REQ_WORKING, ROLE_QA_CHECK_DOC);

        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_TASK_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_DD_OPEN+"#"+ID));
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, DND_OPEN2CLOSED+"#"+ID, TD_REQ_OPEN+"#"+ID));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_OPEN2CLOSED+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new AddedConstraintsEvt(ID, TD_TASK_OPEN+"#"+ID, Map.of("RuleName", "Description")));
        wfiWrapper.handle(new AddedConstraintsEvt(ID, TD_REQ_OPEN+"#"+ID, Map.of("RuleName", "Description")));


        WorkflowTask wftClosed = wfi.getWorkflowTask(TD_TASK_CLOSED+"#"+ID);
        WorkflowTask wftWorking = wfi.getWorkflowTask(TD_REQ_WORKING+"#"+ID);
        assertEquals(1, wftClosed.getInput().size());
        assertEquals(1, wftWorking.getInput().size());
        assertEquals(2, dni.getMappingReports().size());
    }

    @Test
    public void testMapOutputsToExpectedInputsComplexWorkflowAdditionalMappingQACheckDocPresent() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();

        wfiWrapper.handle(new CreatedWorkflowEvt(ID, List.of(a), "", new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(TD_TASK_OPEN, ROLE_QA_CHECK_DOC, TD_TASK_CLOSED, ROLE_QA_CHECK_DOC);
        dni.getDefinition().addMapping(TD_REQ_OPEN, ROLE_QA_CHECK_DOC, TD_REQ_WORKING, ROLE_QA_CHECK_DOC);

        wfiWrapper.handle(new AddedConstraintsEvt(ID, TD_TASK_OPEN+"#"+ID, Map.of("RuleName", "Description")));
        wfiWrapper.handle(new AddedConstraintsEvt(ID, TD_REQ_OPEN+"#"+ID, Map.of("RuleName", "Description")));
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

        wfiWrapper.handle(new CreatedWorkflowEvt(ID, List.of(a), "", new ComplexWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, DND_KICKOFF+"#"+ID, rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI

        // add additional mappings
        WorkflowInstance wfi = wfiWrapper.getWorkflowInstance();
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(DND_OPEN2CLOSED+"#"+ID);
        dni.getDefinition().addMapping(List.of(MappingDefinition.Pair.of(TD_TASK_OPEN, ROLE_QA_CHECK_DOC), MappingDefinition.Pair.of(TD_REQ_OPEN, ROLE_QA_CHECK_DOC)),
                List.of(MappingDefinition.Pair.of(TD_TASK_CLOSED, ROLE_QA_CHECK_DOC), MappingDefinition.Pair.of(TD_REQ_WORKING, ROLE_QA_CHECK_DOC)), MappingDefinition.MappingType.ALL);

        wfiWrapper.handle(new AddedConstraintsEvt(ID, TD_TASK_OPEN+"#"+ID, Map.of("RuleName", "Description")));
        wfiWrapper.handle(new AddedConstraintsEvt(ID, TD_REQ_OPEN+"#"+ID, Map.of("RuleName", "Description")));
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
