package impactassessment.query.snapshot;

import impactassessment.query.MockDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static impactassessment.query.snapshot.CLTool.Action.STEP;
import static impactassessment.query.snapshot.CLTool.Action.STORE;

@Component
@XSlf4j
@RequiredArgsConstructor
public class Snapshotter {

    private final EventStore eventStore;
    private final CLTool cli;

    private int sequenceNumber = 0;
    private ExecutorService executor = Executors.newFixedThreadPool(1);
//    private static boolean isInReplay = false;

    public Future<MockDatabase> replayEventsUntil(Instant timestamp) {
        /*
        if (isInReplay) {
            log.info("[SNP] replay is currently running");
            return null;
        }
        isInReplay = true;
        */
        Callable<MockDatabase> callable = new ReplayCallable(eventStore, ++sequenceNumber, timestamp, cli);
        Future<MockDatabase> future = executor.submit(callable);
        return future;
    }

    /**
     * Needed for Test-Fixture
     * openStream on eventStore would cause a java.lang.UnsupportedOperationException
     */
    @Deprecated
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

        /**
         * Needed for Test-Fixture
         * openStream would cause a java.lang.UnsupportedOperationException
         */
        @Deprecated
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
            eventStream.takeWhile(x -> x.getTimestamp().isBefore(timestamp)).forEach(m -> {
                log.debug("[SNP] replay number {} from {} handle {}", id, m.getTimestamp(), m.getPayload());
                mockDB.handle(m);
            });
            log.debug("[SNP] Replayed content:");
            mockDB.print();
            return mockDB;
        }

        /*
        // eventStream is infinite, so we would need terminal operation on the stream
        // as we don't know how far the user wants to step into the future, not sure how to implement this
        @Override
        public MockDatabase call() {
            AtomicReference<CLTool.Action> action = new AtomicReference<>();
            action.set(STEP);
            eventStream.forEach(m -> {
                log.debug("[SNP] replay number {} from {}: {}", id, m.getTimestamp(), m.getPayload());
                if (m.getTimestamp().isBefore(timestamp)) {
                    log.debug("[SNP] handle next msg");
                    mockDB.handle(m);
                } else {
                    if (action.get() != STORE) {
                        CompletableFuture<CLTool.Action> completableFuture = cli.readAction();
                        try {
                            action.set(completableFuture.get());
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    switch (action.get()) {
                        case STORE:
                            // don't handle messages
                            break;
                        case PRINT:
                            mockDB.print();
                        default:
                            log.debug("[SNP] handle next msg");
                            mockDB.handle(m);
                    }
                }
            });
            log.debug("[SNP] return snapshot");
            isInReplay = false;
            return mockDB;
        }
        */
    }
}
