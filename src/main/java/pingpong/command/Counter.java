package pingpong.command;

import pingpong.api.*;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.lang.invoke.MethodHandles;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Profile("command")
public class Counter {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;
    private int count;

    public Counter() {
        log.debug("empty constructor invoked");
    }

    // Command Handlers

    @CommandHandler
    public Counter(CreateCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CreatedEvt(cmd.getId(), cmd.getAmount()));
    }

    @CommandHandler
    public void handle(IncreaseCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new IncreasedEvt(cmd.getId(), cmd.getAmount()));
    }

    @CommandHandler
    public void handle(DecreaseCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new DecreasedEvt(cmd.getId(), cmd.getAmount()));
    }

    // Event Handlers

    @EventSourcingHandler
    public void on(CreatedEvt evt) {
        log.debug("applying {}", evt);
        id = evt.getId();
        count = evt.getAmount();
        log.debug("new count: {}", count);
    }

    @EventSourcingHandler
    public void on(IncreasedEvt evt) {
        log.debug("applying {}", evt);
        count += evt.getAmount();
        log.debug("new count: {}", count);
    }

    @EventSourcingHandler
    public void on(DecreasedEvt evt) {
        log.debug("applying {}", evt);
        count -= evt.getAmount();
        log.debug("new count: {}", count);
    }
}
