package impactassessment.command;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import java.io.Serializable;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import impactassessment.api.Commands.RefreshFrontendDataCmd;
import impactassessment.api.Events.*;
import lombok.extern.slf4j.Slf4j;

@Aggregate
@Profile("command")
@Slf4j
public class RefreshForwarderAggregate implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@AggregateIdentifier
    String id = RefreshForwarderAggregate.class.getSimpleName();

	public RefreshForwarderAggregate() {}
	
	@CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(RefreshFrontendDataCmd cmd) {
		//log.info("[AGG] handling {}", cmd);
		apply(new RefreshedTriggerEvent(cmd.getId()));
	}
	
    @EventSourcingHandler
    public void on(RefreshFrontendDataCmd evt) {
       // log.debug("[AGG] applying {}", evt);
    }
}
