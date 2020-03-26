package impactassessment.query;

import impactassessment.rulebase.RuleBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import impactassessment.api.*;

@Component
@XSlf4j
@RequiredArgsConstructor
@Profile("query")
@ProcessingGroup("projection")
public class WorkflowProjection {

    private final MockDatabase mockDB;

    @EventHandler
    public void on(CreatedWorkflowEvt event) {
        log.debug("projecting {}", event);
        WorkflowModel m = mockDB.createAndPutWorkflowModel(event.getId());
        m.handle(event);
    }

    @EventHandler
    public void on(CreatedWorkflowInstanceOfEvt event/*, ReplayStatus replayStatus, @Timestamp Instant t, @SequenceNumber Long l*/) {
        log.debug("projecting {}", event);
        mockDB.getWorkflowModel(event.getId()).handle(event);
    }

    @EventHandler
    public void on(EnabledTasksAndDecisionsEvt event/*, ReplayStatus replayStatus, @Timestamp Instant t, @SequenceNumber Long l*/) {
        log.debug("projecting {}", event);
        mockDB.getWorkflowModel(event.getId()).handle(event);
    }

    @EventHandler
    public void on(CompletedDataflowOfDecisionNodeInstanceEvt event/*, ReplayStatus replayStatus, @Timestamp Instant t, @SequenceNumber Long l*/) {
        log.debug("projecting {}", event);
        mockDB.getWorkflowModel(event.getId()).handle(event);
    }

    @EventHandler
    public void on(AddedQACheckDocumentsArtifactOutputsEvt event/*, ReplayStatus replayStatus, @Timestamp Instant t, @SequenceNumber Long l*/) {
        log.debug("projecting {}", event);
        mockDB.getWorkflowModel(event.getId()).handle(event);
    }

    @EventHandler
    public void on(DeletedEvt evt) {
        log.debug("projecting {}", evt);
        mockDB.delete(evt.getId());
    }

    @QueryHandler
    public FindResponse handle(FindQuery query) {
        log.debug("handle {}", query);
        System.out.println(mockDB.getWorkflowModel(query.getId()).toString()); // TODO remove
        String id = query.getId();
        return new FindResponse(id, 404); // TODO replace
    }

    @ResetHandler
    public void reset() {
        log.debug("reset view db");
        mockDB.reset();
    }
}
