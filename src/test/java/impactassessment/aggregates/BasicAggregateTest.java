package impactassessment.aggregates;

import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import org.junit.Test;

public class BasicAggregateTest extends AbstractFixtureTest {

    @Test
    public void testAdd() {
        Artifact a = MockService.mockArtifact(id);
        fixture.givenNoPriorActivity()
                .when(new AddArtifactCmd(id, a))
                .expectEvents(new AddedArtifactEvt(id, a));
    }

    @Test
    public void testAddCompleteActivate() {
        Artifact a = MockService.mockArtifact(id);
        fixture.given(new AddedArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, a))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
//                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testDelete() {
        Artifact a = MockService.mockArtifact(id);
        fixture.given(new AddedArtifactEvt(id, a))
                .when(new DeleteCmd(id))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new DeletedEvt(id))
                .expectMarkedDeleted();
    }

}
