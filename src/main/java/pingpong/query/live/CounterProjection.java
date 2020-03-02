package pingpong.query.live;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pingpong.api.*;
import pingpong.query.CounterModel;
import pingpong.query.snapshot.MyEventHandler;
import pingpong.rulebase.RuleEvaluation;

import java.time.Instant;

@Component
@XSlf4j
@RequiredArgsConstructor
@Profile("query")
@ProcessingGroup("live")
public class CounterProjection {

    private final RuleEvaluation re = new RuleEvaluation();
    private final MyEventHandler handler;

    private CounterModel model = CounterModel.getInstance();

    @EventHandler
    public void on(CreatedEvt event, @Timestamp Instant t, @SequenceNumber Long l) {
        log.debug("projecting (live) {}", event);
        handler.handle(event);
        // trigger rule in rule engine?
    }

    @EventHandler
    public void on(IncreasedEvt event, ReplayStatus replayStatus, @Timestamp Instant t, @SequenceNumber Long l) {
        log.debug("projecting (live) {}", event);
        handler.handle(event);
        if (!replayStatus.isReplay()) {
            re.insertAndFire();
        }
    }

    @EventHandler
    public void on(DecreasedEvt event, @Timestamp Instant t, @SequenceNumber Long l) {
        log.debug("projecting (live) {}", event);
        handler.handle(event);
        // trigger rule in rule engine?
    }

    @QueryHandler
    public FindResponse handle(FindQuery query) {
        log.debug("handle (live) {}", query);
        model.print(); // TODO remove
        String id = query.getId();
        return new FindResponse(id, model.getCountOf(id));
    }

    @ResetHandler
    public void reset() {
        log.debug("reset view db (live)");
        model.reset();
    }
}
