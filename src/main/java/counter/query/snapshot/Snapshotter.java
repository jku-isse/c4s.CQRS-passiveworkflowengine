package counter.query.snapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;
import counter.api.CreatedEvt;
import counter.api.DecreasedEvt;
import counter.api.IncreasedEvt;
import counter.query.CounterModel;

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
        ReplayRunnable r = getReplayRunnable();
        r.init(++sequenceNumber, timestamp);
        new Thread(r).start();
    }

    private ReplayRunnable getReplayRunnable() {
        return new ReplayRunnable(eventStore);
    }

    @RequiredArgsConstructor
    public static class ReplayRunnable implements Runnable {

        private final EventStore eventStore;

        private CLTool cli;
        private int id;
        private Instant timestamp;
        private CounterModel model;

        public void init(int id, Instant timestamp) {
            this.id = id;
            this.timestamp = timestamp;
            this.model = new CounterModel();
            this.cli = new CLTool();
        }

        @Override
        public void run() {
            eventStore.openStream(null).asStream().forEach(m -> {
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
                            // TODO
                            break;
                        case STOP:
                            // TODO
                            break;
                        case PRINT:
                            model.print();
                            break;
                        case STEP:
                            // do nothing
                            break;
                        default:
                            log.error("Snapshotter received invalid action: {}", action);
                    }
                    model.handle(m);
                }
            });
        }

    }
}
