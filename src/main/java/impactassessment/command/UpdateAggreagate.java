package impactassessment.command;

import impactassessment.api.*;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;


@Slf4j
@Aggregate
public class UpdateAggreagate {

    @AggregateIdentifier
    String id = "update";
    private Set<String> aggregateIds = new HashSet<>();

    public UpdateAggreagate() {
        log.debug("[AGG] empty constructor WorkflowAggregate invoked");
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(AddIdCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedIdEvt(cmd.getId(), cmd.getWfAggregateId()));
    }

    @CommandHandler
    public void handle(RemoveIdCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new RemovedIdEvt(cmd.getId(), cmd.getWfAggregateId()));
    }

    @CommandHandler
    public void handle(UpdateWorkflowsCmd cmd, CommandGateway commandGateway) {
        log.info("[AGG] handling {}", cmd);
        for (String id : aggregateIds) {
            commandGateway.send(new ArtifactUpdateCmd(id, cmd.getArtifact()));
        }
    }

    @EventSourcingHandler
    public void on(AddedIdEvt evt) {
        log.debug("[AGG] applying {}", evt);
        aggregateIds.add(evt.getWfAggregateId());
    }

    @EventSourcingHandler
    public void on(RemovedIdEvt evt) {
        log.debug("[AGG] applying {}", evt);
        aggregateIds.remove(evt.getWfAggregateId());
    }

}
