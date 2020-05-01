package impactassessment.snapshots;

import impactassessment.api.*;
import impactassessment.command.WorkflowAggregate;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import impactassessment.model.workflowmodel.IBranchInstance;
import impactassessment.query.MockDatabase;
import impactassessment.model.WorkflowModel;
import impactassessment.query.snapshot.CLTool;
import impactassessment.query.snapshot.Snapshotter;
import impactassessment.rulebase.RuleBaseService;
import impactassessment.model.definition.WPManagementWorkflow;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@SpringBootTest(classes = SpringTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class SnapshotTest {

    private FixtureConfiguration<WorkflowAggregate> fixture;
    @Autowired
    private RuleBaseService ruleBaseService;
    @Autowired
    private CLTool cli;
    private WPManagementWorkflow workflow;
    private String id;

    @Before
    public void setup() {
        fixture = new AggregateTestFixture<>(WorkflowAggregate.class);
        fixture.registerInjectableResource(ruleBaseService);
        workflow = new WPManagementWorkflow();
        workflow.initWorkflowSpecification();
        workflow.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        id = "Test-Workflow";
    }

    @Test
    public void testAddCompleteActivate() {
        Artifact a = MockService.mockArtifact(id);
        fixture.given(new AddedArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, a))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testSnapshotStateEqualAggregateState() {
        Artifact a = MockService.mockArtifact(id);
        fixture.given(new AddedArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, a))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.now(), eventStream);
                    WorkflowModel snapshotState = null;
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
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.parse("2020-03-17T08:30:00.00Z"), eventStream);
                    WorkflowModel snapshotState = null;
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
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.parse("2020-03-19T08:30:00.00Z"), eventStream);
                    WorkflowModel snapshotState = null;
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
