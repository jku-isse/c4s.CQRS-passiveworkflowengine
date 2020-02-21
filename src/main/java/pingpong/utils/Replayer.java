package pingpong.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Replayer {

    private final EventProcessingConfiguration configuration;
    private final EventStore eventStore;

    private String name = "live";
    private String time = "2020-02-18T13:31:00.00Z";

    private TrackingToken snapshotToken;

    public void replay() {
        replay(name, time);
    }
    public void replay(String name) {
        replay(name, time);
    }
    public void replay(String name, String snapshotTime) {
        Instant i;
        try {
            i = Instant.parse(snapshotTime);
        } catch (DateTimeParseException e) {
            i = Instant.parse(time);
        }
        snapshotToken = eventStore.createTokenAt(i);
        configuration.eventProcessorByProcessingGroup(name, TrackingEventProcessor.class)
                .ifPresent(trackingEventProcessor -> {
                    trackingEventProcessor.shutDown();
                    trackingEventProcessor.resetTokens();
                    trackingEventProcessor.start();
                });
    }

    public boolean isAtSnapshotPosition(Instant t) throws NullPointerException {
        TrackingToken currentToken = eventStore.createTokenAt(t);
        // log.debug("Compare {} == {}", snapshotToken.position().getAsLong(), currentToken.position().getAsLong());
        return snapshotToken.position().getAsLong() == currentToken.position().getAsLong();
    }

    public void setSnapshotTokenAt(Instant t) {
        t = t.plusMillis(100);
        snapshotToken = eventStore.createTokenAt(t);
        // log.debug("New token: {}", snapshotToken.position().getAsLong());
    }
}
