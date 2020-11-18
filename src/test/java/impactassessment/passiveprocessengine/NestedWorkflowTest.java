package impactassessment.passiveprocessengine;

import impactassessment.api.Events.*;
import impactassessment.api.Commands.*;
import impactassessment.exampleworkflows.DronologyWorkflowFixed;
import impactassessment.exampleworkflows.NestedWorkflow;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import org.junit.Before;
import org.junit.Test;
import passiveprocessengine.definition.ArtifactType;
import passiveprocessengine.definition.ArtifactTypes;
import passiveprocessengine.instance.ResourceLink;

import java.util.List;

public class NestedWorkflowTest {

    private final String ID = "test";
    private IJiraArtifact a;
    private ResourceLink rl;

    @Before
    public void setup() {
        a = JiraMockService.mockArtifact(ID);
        rl = new ResourceLink("test", "test", "test", "test", "test", "test");
    }

    @Test
    public void testNestedWorkflow() {
        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
        wfiWrapper.handle(new CreatedWorkflowEvt(ID, List.of(a), "", new NestedWorkflow()));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new AddedOutputEvt(
                ID,
                "Open#test",
                new ResourceLink(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK, "dummy", "dummy", "dummy", "dummy", "dummy"),
                "irrelevantForTest",
                new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK)
        ));
        wfiWrapper.handle(new ActivatedInOutBranchEvt(ID, "open2inProgress#test", "Open#test", "inProgressIn"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2inProgress#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
        wfiWrapper.handle(new ActivatedInBranchEvt(ID, "inProgress2resolved#test", "In Progress#test"));
        wfiWrapper.handle(new ActivatedOutBranchEvt(ID, "inProgress2resolved#test", "resolvedIn"));
        wfiWrapper.handle(new CompletedDataflowEvt(ID, "inProgress2resolved#test", rl));

        WorkflowInstanceWrapper nestedWfiWrapper = new WorkflowInstanceWrapper();
        nestedWfiWrapper.handle(new CreatedSubWorkflowEvt("Nested#In Progress#test", ID, "In Progress#test", "", new DronologyWorkflowFixed()));
        System.out.println("x");
    }


}
