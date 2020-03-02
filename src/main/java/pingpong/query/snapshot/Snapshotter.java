package pingpong.query.snapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.common.stream.BlockingStream;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;
import pingpong.api.CreatedEvt;
import pingpong.api.DecreasedEvt;
import pingpong.api.IncreasedEvt;
import pingpong.query.CounterModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@XSlf4j
@RequiredArgsConstructor
public class Snapshotter {
    private final EventStore eventStore;
    private final MyEventHandler handler;

    private List<BlockingStream> eventStreams = new ArrayList<>();
    private CLITool cli = new CLITool();
    private CounterModel model = CounterModel.getInstance();

    public void replayEventsUntil(Instant timestamp) {
        new Thread(() -> eventStore.openStream(null).asStream().forEach(m -> {
            ckeckProgress(m, timestamp);
            handle(m);
        })).start();
    }

    private void ckeckProgress(TrackedEventMessage<?> m, Instant timestamp) {
        if (m.getTimestamp().isAfter(timestamp)) {
            model.print();
            CompletableFuture<CLITool.Action> completableFuture = cli.readAction();
            CLITool.Action action = null;
            try {
                action = completableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            switch (action) {
                case STORE:
                    model.createSnapshot();
                    break;
                case STOP:
                    // TODO
                    break;
                default:
                    // do nothing
            }
        }
    }

    private void handle(TrackedEventMessage<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof CreatedEvt) {
            handler.handle((CreatedEvt)payload);
        } else if (payload instanceof IncreasedEvt) {
            handler.handle((IncreasedEvt)payload);
        } else if (payload instanceof DecreasedEvt) {
            handler.handle((DecreasedEvt)payload);
        } else {
            log.error("unknown event");
        }
    }
}
