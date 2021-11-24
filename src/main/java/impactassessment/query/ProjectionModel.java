package impactassessment.query;

import impactassessment.api.Events.*;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.axonframework.eventhandling.EventMessage;
import org.springframework.stereotype.Component;

import artifactapi.IArtifactRegistry;
import passiveprocessengine.instance.WorkflowInstance;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


@Component
@Slf4j
public class ProjectionModel {

	protected final IArtifactRegistry artifactRegistry;
	
    @Getter
	protected
    ConcurrentMap<String, WorkflowInstanceWrapper> db;

    public ProjectionModel(IArtifactRegistry artifactRegistry) {
        db = new ConcurrentHashMap<>();
        this.artifactRegistry = artifactRegistry;
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

    public synchronized WorkflowInstanceWrapper createAndPutWorkflowModel(String id) {
        WorkflowInstanceWrapper suc = db.put(id, new WorkflowInstanceWrapper(artifactRegistry));
        return db.get(id);
    }

    public synchronized WorkflowInstanceWrapper delete(String id) {
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

    public Collection<WorkflowInstance> getWfis() {
        return db.values().stream()
                .map(WorkflowInstanceWrapper::getWorkflowInstance)
                .collect(Collectors.toList());
    }

    public Optional<WorkflowInstance> getWfi(String id) {
        return db.values().stream()
                .map(WorkflowInstanceWrapper::getWorkflowInstance)
                .filter(wfi -> wfi.getId().equals(id))
                .findAny();
    }
}
