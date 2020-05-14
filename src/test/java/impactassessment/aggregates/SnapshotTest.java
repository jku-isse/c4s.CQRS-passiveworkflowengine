package impactassessment.aggregates;

import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.query.MockDatabase;
import impactassessment.query.snapshot.CLTool;
import impactassessment.query.snapshot.Snapshotter;
import org.axonframework.eventhandling.EventMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class SnapshotTest extends AbstractFixtureTest {

    @Autowired
    private CLTool cli;

    @Test
    public void testAddCompleteActivate() {
        Artifact a = MockService.mockArtifact(id);
        fixture.given(new AddedArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, a))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testSnapshotStateEqualAggregateState() {
        Artifact a = MockService.mockArtifact(id);
        fixture.given(new AddedArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, a))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.now(), eventStream);
                    WorkflowInstanceWrapper snapshotState = null;
                    try {
                        snapshotState = future.get().getWorkflowModel(id);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        Assert.fail();
                    }

                    Assert.assertNotNull(state.getModel());
                    Assert.assertTrue(snapshotState.equals(state.getModel()));
                });
    }

    @Test
    public void testSnapshotBeforeFirstEvent() {
        Artifact a = MockService.mockArtifact(id);
        fixture.givenCurrentTime(Instant.parse("2020-03-18T08:30:00.00Z"))
                .andGiven(new AddedArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, a))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.parse("2020-03-17T08:30:00.00Z"), eventStream);
                    WorkflowInstanceWrapper snapshotState = null;
                    try {
                        snapshotState = future.get().getWorkflowModel(id);
                    } catch (InterruptedException | ExecutionException e) {
                        Assert.fail();
                    }

                    Assert.assertNull(snapshotState);
                });
    }

    @Test
    public void testSnapshotStateNotEqualAggregateState() {
        Artifact a = MockService.mockArtifact(id);
        fixture.givenCurrentTime(Instant.parse("2020-03-18T08:30:00.00Z"))
                .andGiven(new AddedArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, a))
                .andGivenCurrentTime(Instant.parse("2020-03-20T08:30:00.00Z"))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.parse("2020-03-19T08:30:00.00Z"), eventStream);
                    WorkflowInstanceWrapper snapshotState = null;
                    try {
                        snapshotState = future.get().getWorkflowModel(id);
                    } catch (InterruptedException | ExecutionException e) {
                        Assert.fail();
                    }

                    Assert.assertNotNull(state.getModel());
                    Assert.assertFalse(snapshotState.equals(state.getModel()));
                });
    }
}
