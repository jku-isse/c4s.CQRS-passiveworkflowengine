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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@XSlf4j
@RequiredArgsConstructor
public class Snapshotter {

    private final EventStore eventStore;

    private Map<Integer, CounterModel> snapshots = new HashMap<>();

    private int sequenceNumber = 0;

    public void replayEventsUntil(Instant timestamp) {
        //snapshots.put(++sequenceNumber, new CounterModel());
        ReplayRunnable r = getReplayRunnable();
        r.init(++sequenceNumber, timestamp);
        new Thread(r).start();
    }

    public ReplayRunnable getReplayRunnable() {
        return new ReplayRunnable(eventStore);
    }

    @RequiredArgsConstructor
    public static class ReplayRunnable implements Runnable {

        private final EventStore eventStore;

        private CLITool cli;
        private int id;
        private Instant timestamp;
        private CounterModel model;

        public void init(int id, Instant timestamp) {
            this.id = id;
            this.timestamp = timestamp;
            this.model = new CounterModel();
            this.cli = new CLITool();
        }

        @Override
        public void run() {
            eventStore.openStream(null).asStream().forEach(m -> {
                userAction(m, timestamp);
                handle(m);
            });
        }

        private void userAction(TrackedEventMessage<?> m, Instant timestamp) {
            if (m.getTimestamp().isAfter(timestamp)) {
                log.info(String.valueOf(id));
                CompletableFuture<CLITool.Action> completableFuture = cli.readAction();
                CLITool.Action action = null;
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
            }
        }

        private void handle(TrackedEventMessage<?> message) {
            Object payload = message.getPayload();
            if (payload instanceof CreatedEvt) {
                model.handle((CreatedEvt)payload);
            } else if (payload instanceof IncreasedEvt) {
                model.handle((IncreasedEvt)payload);
            } else if (payload instanceof DecreasedEvt) {
                model.handle((DecreasedEvt)payload);
            } else {
                log.error("unknown event: {}", payload.getClass().getSimpleName());
            }
        }
    }
}
