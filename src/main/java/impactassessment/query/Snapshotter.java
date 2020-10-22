package impactassessment.query;

import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericTrackedDomainEventMessage;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class Snapshotter {
    private final EventStore eventStore;

    private ProjectionModel projectionModel;
    private CompletableFuture<ProjectionModel> futureDB = new CompletableFuture<>();
    private ReplayRunnable worker;

    private CompletableFuture<Action> futureAction = new CompletableFuture<>();
    private Instant jumpTimestamp;

    private double head;
    private double cur;

    public boolean start(Instant timestamp) {
        if (isNotValidTimestamp(timestamp)) return false;
        stop(); // stop last thread if still running

        futureDB = new CompletableFuture<>();
        futureAction = new CompletableFuture<>();
        futureAction.complete(Action.STEP);
        projectionModel = new ProjectionModel();

        worker = new ReplayRunnable(eventStore, timestamp);
        worker.start();
        return true;
    }

    private void stop() {
        if (worker != null) {
            futureAction.complete(Action.QUIT);
            try {
                worker.join(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public List<WorkflowInstanceWrapper> getState() {
        ConcurrentMap<String, WorkflowInstanceWrapper> data = null;
        try {
            data = futureDB.get().getDb();
            futureDB = new CompletableFuture<>();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return data.entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList());
    }

    public boolean step() {
        head = eventStore.createHeadToken().position().getAsLong();
        if (cur >= head) {
            quit();
            return false;
        }
        futureAction.complete(Action.STEP);
        return true;
    }

    public boolean jump(Instant time) {
        if (isNotValidTimestamp(time)) return false;
        futureAction.complete(Action.JUMP);
        jumpTimestamp = time;
        return true;
    }

    private boolean isNotValidTimestamp(Instant time) {
        head = eventStore.createHeadToken().position().getAsLong();
        TrackingToken t = eventStore.createTokenAt(time);
        if (t == null) {
            return true;
        }
        OptionalLong curPos = t.position();
        if (curPos.isEmpty() || curPos.getAsLong() >= (head-2)) { // not sure why -2 is needed
            return true;
        }
        return false;
    }

    public void quit() {
        futureAction.complete(Action.QUIT);
    }

    public double getProgress() {
        return cur / head;
    }

    public int eventsMissing() {
        return (int)(head - cur);
    }

    private Action getAction() {
        Action a = Action.JUMP;
        try {
           a = futureAction.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (!a.equals(Action.JUMP)) {
            futureAction = new CompletableFuture<>();
        }
        return a;
    }

    public class ReplayRunnable extends Thread {

        private Stream<? extends EventMessage<?>> eventStream;
        private Instant timestamp;

        public ReplayRunnable(EventStore eventStore, Instant timestamp) {
            this.timestamp = timestamp;
            this.eventStream = eventStore.openStream(null).asStream();
        }

        @Override
        public void run() {
            eventStream.forEach(e -> {
                if (e.getTimestamp().isBefore(timestamp)) {
                    projectionModel.handle(e);
                } else {
                    switch (getAction()) {
                        case STEP:
                            cur = ((GenericTrackedDomainEventMessage)e).trackingToken().position().getAsLong();
                            futureDB.complete(projectionModel);
                            projectionModel.handle(e);
                            break;
                        case JUMP:
                            cur = ((GenericTrackedDomainEventMessage)e).trackingToken().position().getAsLong();
                            if (e.getTimestamp().isBefore(jumpTimestamp)) {
                                projectionModel.handle(e);
                            } else {
                                futureDB.complete(projectionModel);
                                projectionModel.handle(e);
                                futureAction = new CompletableFuture<>();
                            }
                            break;
                        case QUIT:
                            log.info("Snapshotter stops");
                            return;
                        default:
                            log.error("Snapshotter: Invalid action!");
                    }
                }
            });
        }
    }

    public enum Action {STEP, JUMP, QUIT}
}
