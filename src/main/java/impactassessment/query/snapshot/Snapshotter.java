package impactassessment.query.snapshot;

import impactassessment.api.IdentifiableEvt;
import impactassessment.query.MockDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;
import impactassessment.query.WorkflowModel;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@XSlf4j
@RequiredArgsConstructor
public class Snapshotter {

    private final EventStore eventStore;

    private int sequenceNumber = 0;

    public void replayEventsUntil(Instant timestamp) {
        ReplayRunnable r = new ReplayRunnable(eventStore, ++sequenceNumber, timestamp);
        new Thread(r).start();
    }

    public static class ReplayRunnable implements Runnable {

        private final EventStore eventStore;
        private final int id;
        private final Instant timestamp;
        private final CLTool cli;
        private MockDatabase mockDB;

        public ReplayRunnable(EventStore eventStore, int id, Instant timestamp) {
            this.eventStore = eventStore;
            this.id = id;
            this.timestamp = timestamp;
            this.mockDB = new MockDatabase();
            this.cli = new CLTool();
        }

        @Override
        public void run() {
            eventStore.openStream(null).asStream().forEach(m -> {
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
        }

    }
}
