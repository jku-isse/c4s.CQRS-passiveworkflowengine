package counter.query;

import lombok.extern.slf4j.XSlf4j;
import counter.api.CreatedEvt;
import counter.api.DecreasedEvt;
import counter.api.IncreasedEvt;
import org.axonframework.eventhandling.TrackedEventMessage;

import java.util.HashMap;
import java.util.Map;

@XSlf4j
public class CounterModel {

    private Map<String, Integer> model;

    public CounterModel() {
        model = new HashMap<>();
    }

    public void handle(CreatedEvt event) {
        model.put(event.getId(), event.getAmount());
    }

    public void handle(IncreasedEvt event) {
        model.put(event.getId(), model.get(event.getId()) + event.getAmount());
    }

    public void handle(DecreasedEvt event) {
        model.put(event.getId(), model.get(event.getId()) - event.getAmount());
    }

    public void handle(TrackedEventMessage<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof CreatedEvt) {
            handle((CreatedEvt)payload);
        } else if (payload instanceof IncreasedEvt) {
            handle((IncreasedEvt)payload);
        } else if (payload instanceof DecreasedEvt) {
            handle((DecreasedEvt)payload);
        } else {
            log.error("unknown event: {}", payload.getClass().getSimpleName());
        }
    }

    public int getCountOf(String id) {
        return model.get(id);
    }

    public void reset() {
        model = new HashMap<>();
    }

    public void print() {
        log.info("***********************************");
        for (Map.Entry<String, Integer> e : model.entrySet()) {
            log.info("id: {}, amount: {}", e.getKey(), e.getValue());
        }
        log.info("***********************************");
    }
}
