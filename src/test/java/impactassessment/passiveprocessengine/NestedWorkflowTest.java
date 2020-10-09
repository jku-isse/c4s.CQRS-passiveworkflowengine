package impactassessment.passiveprocessengine;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.passiveprocessengine.definition.ArtifactType;
import impactassessment.passiveprocessengine.definition.ArtifactTypes;
import impactassessment.passiveprocessengine.instance.ResourceLink;
import impactassessment.passiveprocessengine.workflows.DronologyWorkflow;
import impactassessment.passiveprocessengine.workflows.DronologyWorkflowFixed;
import impactassessment.passiveprocessengine.workflows.NestedWorkflow;
import impactassessment.registry.ProcessDefintionObject;
import org.junit.Before;
import org.junit.Test;

public class NestedWorkflowTest {

    private final String ID = "test";
    private IJiraArtifact a;
    private ResourceLink rl;

    @Before
    public void setup() {
        a = JiraMockService.mockArtifact(ID);
        rl = ResourceLink.of(a);
    }

    @Test
    public void testNestedWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(ID, a, new ProcessDefintionObject(NestedWorkflow.WORKFLOW_TYPE, new NestedWorkflow(), null)));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new AddedAsOutputEvt(ID, "Open#test", new ResourceLink(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK, "dummy", "dummy", "dummy", "dummy", "dummy"), "irrelevantForTest", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK)));
        wfiWrapper.handle(new ActivatedInOutBranchEvt(ID, "open2inProgress#test", "Open#test", "inProgressIn"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2inProgress#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, "inProgress2resolved#test", "In Progress#test"));
        wfiWrapper.handle(new ActivatedOutBranchEvt(ID, "inProgress2resolved#test", "resolvedIn"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "inProgress2resolved#test", rl));

        WorkflowInstanceWrapper nestedWfiWrapper = new WorkflowInstanceWrapper();
        nestedWfiWrapper.handle(new CreatedChildWorkflowEvt("Nested#In Progress#test", ID, "In Progress#test", new ProcessDefintionObject(DronologyWorkflowFixed.WORKFLOW_TYPE, new DronologyWorkflowFixed(), null)));
        System.out.println("x");
    }


}
