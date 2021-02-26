//package impactassessment.passiveprocessengine;
//
//import artifactapi.IArtifact;
//import artifactapi.ResourceLink;
//import artifactapi.jira.IJiraArtifact;
//import impactassessment.api.Events.*;
//import impactassessment.artifactconnector.jira.mock.JiraMockService;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.util.*;
//
//public class NestedWorkflowTest {
//
//    private final String ID = "test";
//    private IJiraArtifact a;
//    private ResourceLink rl;
//
//    @Before
//    public void setup() {
//        a = JiraMockService.mockArtifact(ID);
//        rl = new ResourceLink("test", "test", "test", "test", "test", "test");
//    }
//
//    @Test
//    public void testNestedWorkflow() {
//        WorkflowInstanceWrapper wfiWrapper = new WorkflowInstanceWrapper();
//        List<Map.Entry<String, IArtifact>> artifacts = new ArrayList<>();
//        artifacts.add(new AbstractMap.SimpleEntry<>("ROLE", a));
////        wfiWrapper.handle(new CreatedWorkflowEvt(ID, artifacts, "", new NestedWorkflow()));
//        wfiWrapper.handle(new CompletedDataflowEvt(ID, "workflowKickOff#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
////        wfiWrapper.handle(new AddedOutputEvt(
////                ID,
////                "Open#test",
////                new ResourceLink(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK, "dummy", "dummy", "dummy", "dummy", "dummy"),
////                "irrelevantForTest",
////                new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK)
////        ));
//        wfiWrapper.handle(new ActivatedInOutBranchEvt(ID, "open2inProgress#test", "Open#test", "inProgressIn"));
//        wfiWrapper.handle(new CompletedDataflowEvt(ID, "open2inProgress#test", rl)); // this adds an output (ResourceLink) to all WFTs created from this DNI
//        wfiWrapper.handle(new ActivatedInBranchEvt(ID, "inProgress2resolved#test", "In Progress#test"));
//        wfiWrapper.handle(new ActivatedOutBranchEvt(ID, "inProgress2resolved#test", "resolvedIn"));
//        wfiWrapper.handle(new CompletedDataflowEvt(ID, "inProgress2resolved#test", rl));
//
//        WorkflowInstanceWrapper nestedWfiWrapper = new WorkflowInstanceWrapper();
//        nestedWfiWrapper.handle(new CreatedSubWorkflowEvt("Nested#In Progress#test", ID, "In Progress#test", "", new DronologyWorkflowFixed(), Collections.emptyList()));
//        System.out.println("x");
//    }
//
//
//}
