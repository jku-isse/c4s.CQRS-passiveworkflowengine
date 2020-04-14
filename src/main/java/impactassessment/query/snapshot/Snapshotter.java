package impactassessment.query.snapshot;

import impactassessment.query.MockDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Component
@XSlf4j
@RequiredArgsConstructor
public class Snapshotter {

    private final EventStore eventStore;
    private final CLTool cli;

    private int sequenceNumber = 0;
    private ExecutorService executor = Executors.newFixedThreadPool(5);

    public Future<MockDatabase> replayEventsUntil(Instant timestamp) {
        Callable<MockDatabase> callable = new ReplayCallable(eventStore, ++sequenceNumber, timestamp, cli);
        Future<MockDatabase> future = executor.submit(callable);
        return future;
    }

    public Future<MockDatabase> replayEventsUntilWithOwnEvents(Instant timestamp, Stream<? extends EventMessage<?>> eventStream) {
        Callable<MockDatabase> callable = new ReplayCallable(eventStream, ++sequenceNumber, timestamp, cli);
        Future<MockDatabase> future = executor.submit(callable);
        return future;
    }

    public static class ReplayCallable implements Callable {

        private Stream<? extends EventMessage<?>> eventStream;
        private int id;
        private Instant timestamp;
        private CLTool cli;
        private MockDatabase mockDB;

        public ReplayCallable(EventStore eventStore, int id, Instant timestamp, CLTool cli) {
            this(id, timestamp, cli);
            this.eventStream = eventStore.openStream(null).asStream();
        }

        public ReplayCallable(Stream<? extends EventMessage<?>> eventStream, int id, Instant timestamp, CLTool cli) {
            this(id, timestamp, cli);
            this.eventStream = eventStream;
        }

        private ReplayCallable(int id, Instant timestamp, CLTool cli) {
            this.id = id;
            this.timestamp = timestamp;
            this.cli = cli;
            this.mockDB = new MockDatabase();
        }

        @Override
        public MockDatabase call() {
            eventStream.forEach(m -> {
                if (m.getTimestamp().isBefore(timestamp)){
                    mockDB.handle(m);
                } else {
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
                            // do nothing
                            break;
                        case PRINT:
                            mockDB.print();
                            break;
                        case STEP:
                            mockDB.handle(m);
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
