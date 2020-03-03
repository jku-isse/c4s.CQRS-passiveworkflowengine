package counter.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import counter.api.*;

@Component
@XSlf4j
@RequiredArgsConstructor
@Profile("query")
@ProcessingGroup("projection")
public class CounterProjection {

    private CounterModel model = new CounterModel();

    @EventHandler
    public void on(CreatedEvt event) {
        log.debug("projecting {}", event);
        model.handle(event);
    }

    @EventHandler
    public void on(IncreasedEvt event/*, ReplayStatus replayStatus, @Timestamp Instant t, @SequenceNumber Long l*/) {
        log.debug("projecting {}", event);
        model.handle(event);
    }

    @EventHandler
    public void on(DecreasedEvt event) {
        log.debug("projecting {}", event);
        model.handle(event);
    }

    @QueryHandler
    public FindResponse handle(FindQuery query) {
        log.debug("handle {}", query);
        model.print(); // TODO remove
        String id = query.getId();
        return new FindResponse(id, model.getCountOf(id));
    }

    @ResetHandler
    public void reset() {
        log.debug("reset view db");
        model.reset();
    }
}
