package impactassessment.aggregates;

public class SnapshotTest extends AbstractFixtureTest {

//    @Autowired
//    private CLIActionProvider cli;
//
//    @Test
//    public void testAddCompleteActivate() {
//        IJiraArtifact a = JiraMockService.mockArtifact(id);
//        fixture.given(new AddedMockArtifactEvt(id, a))
//                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, ResourceLink.of(a)))
//                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
////                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
//                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
//                .expectSuccessfulHandlerExecution();
//    }
//
//    @Test
//    public void testSnapshotStateEqualAggregateState() {
//        IJiraArtifact a = JiraMockService.mockArtifact(id);
//        fixture.given(new AddedMockArtifactEvt(id, a))
//                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, ResourceLink.of(a)))
//                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
////                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
//                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
//                .expectSuccessfulHandlerExecution()
//                .expectState(state -> {
//                    // create an event stream out of the fixture and pass it to the snapshotter
//                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
//                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
//                    Future<Map<String, WorkflowInstanceWrapper>> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.now(), eventStream);
//                    WorkflowInstanceWrapper snapshotState = null;
//                    try {
//                        snapshotState = future.get().get(id);
//                    } catch (InterruptedException | ExecutionException e) {
//                        e.printStackTrace();
//                        Assert.fail();
//                    }
//
//                    Assert.assertNotNull(state.getModel());
//                    Assert.assertTrue(snapshotState.equals(state.getModel()));
//                });
//    }
//
//    @Test
//    public void testSnapshotBeforeFirstEvent() {
//        IJiraArtifact a = JiraMockService.mockArtifact(id);
//        fixture.givenCurrentTime(Instant.parse("2020-03-18T08:30:00.00Z"))
//                .andGiven(new AddedMockArtifactEvt(id, a))
//                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, ResourceLink.of(a)))
//                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
////                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
//                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
//                .expectSuccessfulHandlerExecution()
//                .expectState(state -> {
//                    // create an event stream out of the fixture and pass it to the snapshotter
//                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
//                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
//                    Future<Map<String, WorkflowInstanceWrapper>> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.parse("2020-03-17T08:30:00.00Z"), eventStream);
//                    WorkflowInstanceWrapper snapshotState = null;
//                    try {
//                        snapshotState = future.get().get(id);
//                    } catch (InterruptedException | ExecutionException e) {
//                        Assert.fail();
//                    }
//
//                    Assert.assertNull(snapshotState);
//                });
//    }
//
//    @Test
//    public void testSnapshotStateNotEqualAggregateState() {
//        IJiraArtifact a = JiraMockService.mockArtifact(id);
//        fixture.givenCurrentTime(Instant.parse("2020-03-18T08:30:00.00Z"))
//                .andGiven(new AddedMockArtifactEvt(id, a))
//                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, ResourceLink.of(a)))
//                .andGivenCurrentTime(Instant.parse("2020-03-20T08:30:00.00Z"))
//                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
////                .andGiven(new ActivatedOutBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
//                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
//                .expectSuccessfulHandlerExecution()
//                .expectState(state -> {
//                    // create an event stream out of the fixture and pass it to the snapshotter
//                    Stream<? extends EventMessage<?>> eventStream = fixture.getEventStore().readEvents(id).asStream();
//                    Snapshotter snapshotter = new Snapshotter(fixture.getEventStore(), cli);
//                    Future<Map<String, WorkflowInstanceWrapper>> future = snapshotter.replayEventsUntilWithOwnEvents(Instant.parse("2020-03-19T08:30:00.00Z"), eventStream);
//                    WorkflowInstanceWrapper snapshotState = null;
//                    try {
//                        snapshotState = future.get().get(id);
//                    } catch (InterruptedException | ExecutionException e) {
//                        Assert.fail();
//                    }
//
//                    Assert.assertNotNull(state.getModel());
//                    Assert.assertFalse(snapshotState.equals(state.getModel()));
//                });
//    }
}
