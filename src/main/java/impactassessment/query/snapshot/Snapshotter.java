package impactassessment.query.snapshot;

import impactassessment.api.IdentifiableEvt;
import impactassessment.query.MockDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;
import impactassessment.query.WorkflowModel;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Component
@XSlf4j
@RequiredArgsConstructor
public class Snapshotter {

    private final EventStore eventStore;

    private int sequenceNumber = 0;
    private ExecutorService executor = Executors.newFixedThreadPool(5);

    public Future<MockDatabase> replayEventsUntil(Instant timestamp) {
        Callable<MockDatabase> callable = new ReplayCallable(eventStore, ++sequenceNumber, timestamp);
        Future<MockDatabase> future = executor.submit(callable);
        return future;
    }

    public Future<MockDatabase> replayEventsUntilWithOwnEvents(Instant timestamp, Stream<? extends EventMessage<?>> eventStream) {
        Callable<MockDatabase> callable = new ReplayCallable(eventStream, ++sequenceNumber, timestamp);
        Future<MockDatabase> future = executor.submit(callable);
        return future;
    }

    public static class ReplayCallable implements Callable {

        private Stream<? extends EventMessage<?>> eventStream;
        private int id;
        private Instant timestamp;
        private CLTool cli;
        private MockDatabase mockDB;

        public ReplayCallable(EventStore eventStore, int id, Instant timestamp) {
            this.eventStream = eventStore.openStream(null).asStream();
            this.id = id;
            this.timestamp = timestamp;
            this.mockDB = new MockDatabase();
            this.cli = new CLTool();
        }

        public ReplayCallable(Stream<? extends EventMessage<?>> eventStream, int id, Instant timestamp) {
            this.eventStream = eventStream;
            this.id = id;
            this.timestamp = timestamp;
            this.mockDB = new MockDatabase();
            this.cli = new CLTool();
        }

        @Override
        public MockDatabase call() {
            eventStream.forEach(m -> {
                mockDB.handle(m);
                if (m.getTimestamp().isAfter(timestamp)) {
                    log.info(String.valueOf(id));
                    CompletableFuture<CLTool.Action> completableFuture = cli.readAction();
                    CLTool.Action action = null;
                    try {
                        action = completableFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    switch (action) {
                        case STORE:
                            // TODO implement
                            break;
                        case STOP:
                            // TODO implement
                            break;
                        case PRINT:
                            mockDB.print();
                            break;
                        case STEP:
                            // do nothing
                            break;
                        default:
                            log.error("Replay received invalid action: {}", action);
                    }
                }
            });
            return mockDB;
        }

    }
}
