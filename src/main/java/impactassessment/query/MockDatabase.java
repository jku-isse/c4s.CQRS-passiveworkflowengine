package impactassessment.query;

import impactassessment.api.IdentifiableEvt;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.EventMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@XSlf4j
public class MockDatabase {

    Map<String, WorkflowModel> db;

    public MockDatabase() {
        db = new HashMap<>();
    }

    public WorkflowModel getWorkflowModel(String id) {
        return db.get(id);
    }

    public WorkflowModel createAndPutWorkflowModel(String id) {
        db.put(id, new WorkflowModel());
        return db.get(id);
    }

    public WorkflowModel delete(String id) {
        return db.remove(id);
    }

    public void handle(EventMessage<?> message) {
        try {
            IdentifiableEvt evt = (IdentifiableEvt) message.getPayload();
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
        for (Map.Entry<String, WorkflowModel> entry : db.entrySet()) {
            System.out.println(entry.getKey()+": "+entry.getValue());
        }
    }
}
