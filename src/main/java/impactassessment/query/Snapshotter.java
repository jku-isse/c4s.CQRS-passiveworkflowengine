package impactassessment.query;

import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.query.MockDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericTrackedDomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class Snapshotter {
    private final EventStore eventStore;

    private MockDatabase mockDB;
    private CompletableFuture<MockDatabase> futureDB = new CompletableFuture<>();
    private ReplayRunnable worker;

    private CompletableFuture<Action> futureAction = new CompletableFuture<>();
    private Instant jumpTimestamp;

    private double head;
    private double cur;

    public void start(Instant timestamp) {
        stop(); // stop last thread if still running

        futureDB = new CompletableFuture<>();
        futureAction = new CompletableFuture<>();
        futureAction.complete(Action.STEP);
        mockDB = new MockDatabase();

        head = eventStore.createHeadToken().position().getAsLong();

        worker = new ReplayRunnable(eventStore, timestamp);
        worker.start();
    }

    private void stop() {
        if (worker != null) {
            futureAction.complete(Action.QUIT);
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public List<WorkflowInstanceWrapper> getState() {
        Map<String, WorkflowInstanceWrapper> data = null;
        try {
            data = futureDB.get().getDb();
            futureDB = new CompletableFuture<>();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return data.entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList());
    }

    public void step() {
        head = eventStore.createHeadToken().position().getAsLong();
        futureAction.complete(Action.STEP);
    }

    public void jump(Instant time) {
        head = eventStore.createHeadToken().position().getAsLong();
        futureAction.complete(Action.JUMP);
        jumpTimestamp = time;
    }

    public void quit() {
        futureAction.complete(Action.QUIT);
    }

    public double getProgress() {
        return cur / head;
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
        private boolean stop;

        public ReplayRunnable(EventStore eventStore, Instant timestamp) {
            this.timestamp = timestamp;
            this.eventStream = eventStore.openStream(null).asStream();
            this.stop = false;
        }

        private void endReached(double d) {
            if (d == head) {
                stop = true;
            }
        }

        @Override
        public void run() {
            eventStream.takeWhile(x -> !stop).forEach(e -> {
                if (e.getTimestamp().isBefore(timestamp)) {
                    mockDB.handle(e);
                } else {
                    switch (getAction()) {
                        case STEP:
                            cur = ((GenericTrackedDomainEventMessage)e).trackingToken().position().getAsLong();
                            futureDB.complete(mockDB);
                            mockDB.handle(e);
                            break;
                        case JUMP:
                            cur = ((GenericTrackedDomainEventMessage)e).trackingToken().position().getAsLong();
                            if (e.getTimestamp().isBefore(jumpTimestamp)) {
                                mockDB.handle(e);
                            } else {
                                futureDB.complete(mockDB);
                                mockDB.handle(e);
                                futureAction = new CompletableFuture<>();
                            }
                            break;
                        case QUIT:
                            stop = true;
                            break;
                        default:
                            log.error("Snapshotter: Invalid action!");
                    }
                }
            });
        }
    }

    public enum Action {STEP, JUMP, QUIT}
}
