package impactassessment.aggregates;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.model.workflowmodel.ResourceLink;
import org.junit.Test;

public class BasicAggregateTest extends AbstractFixtureTest {

    @Test
    public void testAdd() {
        IJiraArtifact a = JiraMockService.mockArtifact(id);
        fixture.givenNoPriorActivity()
                .when(new AddMockArtifactCmd(id, a))
                .expectEvents(new AddedMockArtifactEvt(id, a));
    }

    @Test
    public void testAddCompleteActivate() {
        IJiraArtifact a = JiraMockService.mockArtifact(id);
        fixture.given(new AddedMockArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, ResourceLink.of(a)))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
//                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testDelete() {
        IJiraArtifact a = JiraMockService.mockArtifact(id);
        fixture.given(new AddedMockArtifactEvt(id, a))
                .when(new DeleteCmd(id))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new DeletedEvt(id))
                .expectMarkedDeleted();
    }

}
