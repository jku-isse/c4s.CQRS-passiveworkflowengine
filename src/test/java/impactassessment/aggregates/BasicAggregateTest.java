package impactassessment.aggregates;

import impactassessment.api.*;
import impactassessment.artifact.base.IArtifact;
import impactassessment.artifact.mock.MockService;
import impactassessment.model.workflowmodel.ResourceLink;
import org.junit.Test;

public class BasicAggregateTest extends AbstractFixtureTest {

    @Test
    public void testAdd() {
        IArtifact a = MockService.mockArtifact(id);
        fixture.givenNoPriorActivity()
                .when(new AddMockArtifactCmd(id, a))
                .expectEvents(new AddedMockArtifactEvt(id, a));
    }

    @Test
    public void testAddCompleteActivate() {
        IArtifact a = MockService.mockArtifact(id);
        fixture.given(new AddedMockArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, ResourceLink.of(a)))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
//                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testDelete() {
        IArtifact a = MockService.mockArtifact(id);
        fixture.given(new AddedMockArtifactEvt(id, a))
                .when(new DeleteCmd(id))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new DeletedEvt(id))
                .expectMarkedDeleted();
    }

}
