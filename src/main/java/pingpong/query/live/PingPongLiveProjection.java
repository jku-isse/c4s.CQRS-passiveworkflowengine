package pingpong.query.live;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pingpong.api.*;
import pingpong.query.PingPongModel;
import pingpong.rulebase.RuleEvaluation;

import java.time.Instant;

//@Configuration
//@Scope("prototype")
@Component
@XSlf4j
@RequiredArgsConstructor
@Profile("query")
@ProcessingGroup("live")
public class PingPongLiveProjection {

    private final RuleEvaluation re = new RuleEvaluation();

    private PingPongModel model = PingPongModel.getInstance();

    @EventHandler
    public void on(CreatedEvt event, @Timestamp Instant t, @SequenceNumber Long l) {
        log.debug("projecting (live) {}", event);
        model.createLive(event.getId(), event.getAmount());
        // trigger rule in rule engine?
    }

    @EventHandler
    public void on(IncreasedEvt event, ReplayStatus replayStatus, @Timestamp Instant t, @SequenceNumber Long l) {
        log.debug("projecting (live) {}", event);
        model.increaseLive(event.getId(), event.getAmount());
        if (!replayStatus.isReplay()) {
            re.insertAndFire();
        }
    }

    @EventHandler
    public void on(DecreasedEvt event, @Timestamp Instant t, @SequenceNumber Long l) {
        log.debug("projecting (live) {}", event);
        model.decreaseLive(event.getId(), event.getAmount());
        // trigger rule in rule engine?
    }

    @QueryHandler
    public FindResponse handle(FindLiveQuery query) {
        log.debug("handle (live) {}", query);
        return null; // TODO
    }

    @ResetHandler
    public void reset() {
        log.debug("reset view db (live)");
        model.resetLive();
    }
}
