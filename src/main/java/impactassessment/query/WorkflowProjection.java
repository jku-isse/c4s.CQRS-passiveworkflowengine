package impactassessment.query;

import impactassessment.model.WorkflowInstanceWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import impactassessment.api.*;

import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("query")
@ProcessingGroup("projection")
public class WorkflowProjection {

    private final MockDatabase mockDB;

    // Event Handlers

    @EventHandler
    public void on(AddedArtifactEvt evt) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper m = mockDB.createAndPutWorkflowModel(evt.getId());
        m.handle(evt);
    }

    @EventHandler
    public void on(IdentifiableEvt evt) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfi = mockDB.getWorkflowModel(evt.getId());
        if (wfi != null) wfi.handle(evt);
    }

    @EventHandler
    public void on(DeletedEvt evt) {
        log.info("[PRJ] projecting {}", evt);
        mockDB.delete(evt.getId());
    }

    // Query Handlers

    @QueryHandler
    public FindResponse handle(FindQuery query) {
        log.debug("[PRJ] handle {}", query);
        System.out.println(mockDB.getWorkflowModel(query.getId()).toString()); // TODO remove
        String id = query.getId();
        return new FindResponse(id, 404); // TODO replace
    }

    @QueryHandler
    public GetStateResponse handle(GetStateQuery query) {
        log.debug("[PRJ] handle {}", query);
        return new GetStateResponse(mockDB.getDb().entrySet().stream()
                .map(entry -> entry.getValue())
                .collect(Collectors.toList()));
    }

    // Reset Handler

    @ResetHandler
    public void reset() {
        log.debug("[PRJ] reset view db");
        mockDB.reset();
    }
}
