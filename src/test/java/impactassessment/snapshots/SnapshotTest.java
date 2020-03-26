package impactassessment.snapshots;

import impactassessment.api.*;
import impactassessment.command.WorkflowAggregate;
import impactassessment.query.MockDatabase;
import impactassessment.query.WorkflowModel;
import impactassessment.query.snapshot.CLTool;
import impactassessment.query.snapshot.Snapshotter;
import impactassessment.rulebase.RuleBaseService;
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

import java.time.Duration;
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

    @Before
    public void setup() {
        fixture = new AggregateTestFixture<>(WorkflowAggregate.class);
        fixture.registerInjectableResource(ruleBaseService);
    }

    @Test
    public void testSnapshotStateEqualAggregateState() throws ExecutionException, InterruptedException {
        String aggregateId = "test_wf";

        fixture.given(new CreatedWorkflowEvt(aggregateId), new EnabledEvt(aggregateId, 0))
                .when(new CompleteCmd(aggregateId))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(aggregateId).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.now(), eventStream);
                    WorkflowModel snapshotState = null;
                    try {
                        snapshotState = future.get().getWorkflowModel(aggregateId);
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
        String aggregateId = "test_wf";

        fixture.givenCurrentTime(Instant.parse("2020-03-18T08:30:00.00Z"))
                .andGiven(new CreatedWorkflowEvt(aggregateId), new EnabledEvt(aggregateId, 0))
                .when(new CompleteCmd(aggregateId))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(aggregateId).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.parse("2020-03-17T08:30:00.00Z"), eventStream);
                    WorkflowModel snapshotState = null;
                    try {
                        snapshotState = future.get().getWorkflowModel(aggregateId);
                    } catch (InterruptedException | ExecutionException e) {
                        Assert.fail();
                    }

                    Assert.assertNull(snapshotState);
                });
    }

    @Test
    public void testSnapshotStateNotEqualAggregateState() {
        String aggregateId = "test_wf";

        fixture.givenCurrentTime(Instant.parse("2020-03-18T08:30:00.00Z"))
                .andGiven(new CreatedWorkflowEvt(aggregateId))
                .andGivenCurrentTime(Instant.parse("2020-03-20T08:30:00.00Z"))
                .andGiven(new EnabledEvt(aggregateId, 0))
                .when(new CompleteCmd(aggregateId))
                .expectSuccessfulHandlerExecution()
                .expectState(state -> {
                    // create an event stream out of the fixture and pass it to the snapshotter
                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(aggregateId).asStream();
                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
                    Future<MockDatabase> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.parse("2020-03-19T08:30:00.00Z"), eventStream);
                    WorkflowModel snapshotState = null;
                    try {
                        snapshotState = future.get().getWorkflowModel(aggregateId);
                    } catch (InterruptedException | ExecutionException e) {
                        Assert.fail();
                    }

                    Assert.assertNotNull(state.getModel());
                    Assert.assertFalse(snapshotState.equals(state.getModel()));
                });
    }
}
