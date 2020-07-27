package impactassessment.query;

import impactassessment.api.DeletedEvt;
import impactassessment.api.IdentifiableEvt;
import impactassessment.model.WorkflowInstanceWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.EventMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MockDatabase {

    private @Getter Map<String, WorkflowInstanceWrapper> db;

    public MockDatabase() {
        db = new HashMap<>();
    }

    public WorkflowInstanceWrapper getWorkflowModel(String id) {
        return db.get(id);
    }

    public WorkflowInstanceWrapper createAndPutWorkflowModel(String id) {
        db.put(id, new WorkflowInstanceWrapper());
        return db.get(id);
    }

    public WorkflowInstanceWrapper delete(String id) {
        return db.remove(id);
    }

    public void handle(EventMessage<?> message) {
        try {
            IdentifiableEvt evt = (IdentifiableEvt) message.getPayload();
            if (evt instanceof DeletedEvt) {
                this.delete(evt.getId());
                return;
            }
            if (getWorkflowModel(evt.getId()) == null) {
                createAndPutWorkflowModel(evt.getId());
            }
            getWorkflowModel(evt.getId()).handle(evt);
        } catch (ClassCastException e) {
            log.error("Invalid event type! "+e.getMessage());
        }
    }

    public void reset() {
        db = new HashMap<>();
    }

    public void print() {
        for (Map.Entry<String, WorkflowInstanceWrapper> entry : db.entrySet()) {
            System.out.println(entry.getKey()+": "+entry.getValue());
        }
    }
}
