package impactassessment.query;

import impactassessment.api.Events.*;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Component
@Slf4j
public class ProjectionModel {

    private @Getter
    ConcurrentMap<String, WorkflowInstanceWrapper> db;

    public ProjectionModel() {
        db = new ConcurrentHashMap<>();
    }

    public int size() {
        return db.size();
    }

    public WorkflowInstanceWrapper getWorkflowModel(String id) {
        return db.get(id);
    }

    public WorkflowInstanceWrapper getOrCreateWorkflowModel(String id) {
        if (getWorkflowModel(id) == null) {
            createAndPutWorkflowModel(id);
        }
        return getWorkflowModel(id);
    }

    public boolean contains(String id) {
        return db.containsKey(id);
    }

    public WorkflowInstanceWrapper createAndPutWorkflowModel(String id) {
        db.put(id, new WorkflowInstanceWrapper());
        return db.get(id);
    }

    public WorkflowInstanceWrapper delete(String id) {
        return db.remove(id);
    }

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof DeletedEvt) {
            this.delete(evt.getId());
            return;
        }
        getOrCreateWorkflowModel(evt.getId()).handle(evt);
    }

    public void handle(EventMessage<?> message) {
        try {
            IdentifiableEvt evt = (IdentifiableEvt) message.getPayload();
            handle(evt);
        } catch (ClassCastException e) {
            log.error("Invalid event type! "+e.getMessage());
        }
    }

    public void reset() {
        db = new ConcurrentHashMap<>();
    }

    public void print() {
        for (ConcurrentMap.Entry<String, WorkflowInstanceWrapper> entry : db.entrySet()) {
            System.out.println(entry.getKey()+": "+entry.getValue());
        }
    }
}
