package impactassessment.query;

import impactassessment.api.CompletedEvt;
import impactassessment.api.CreatedWorkflowEvt;
import impactassessment.api.EnabledEvt;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.TrackedEventMessage;

import java.util.HashMap;
import java.util.Map;

@XSlf4j
public class MockDatabase {

    Map<String, WorkflowModel> db;

    public MockDatabase() {
        db = new HashMap<>();
    }

    public WorkflowModel getWorkflowModel(String id) {
        WorkflowModel m = db.get(id);
        if (m == null) {
            m = db.put(id, new WorkflowModel());
        }
        return m;
    }

    public void handle(TrackedEventMessage<?> message) {

        // TODO make events inherit from an IdentifiableEvt interfave that provides getId()
        //  --> makes typechecking here not needed

        Object payload = message.getPayload();
        if (payload instanceof CreatedWorkflowEvt) {
            CreatedWorkflowEvt evt = (CreatedWorkflowEvt) payload;
            getWorkflowModel((evt).getId()).handle(evt);
        } else if (payload instanceof EnabledEvt) {
            EnabledEvt evt = (EnabledEvt) payload;
            getWorkflowModel((evt).getId()).handle(evt);
        } else if (payload instanceof CompletedEvt) {
            CompletedEvt evt = (CompletedEvt) payload;
            getWorkflowModel((evt).getId()).handle(evt);
        } else {
            log.error("unknown event: {}", payload.getClass().getSimpleName());
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
