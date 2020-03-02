package pingpong.query.snapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.stereotype.Component;
import pingpong.api.CreatedEvt;
import pingpong.api.DecreasedEvt;
import pingpong.api.IncreasedEvt;
import pingpong.query.CounterModel;

@XSlf4j
@RequiredArgsConstructor
@Component
public class MyEventHandler {
    private CounterModel model = CounterModel.getInstance();

    public void handle(CreatedEvt event) {
        model.create(event.getId(), event.getAmount());
    }

    public void handle(IncreasedEvt event) {
        model.increase(event.getId(), event.getAmount());
    }

    public void handle(DecreasedEvt event) {
        model.decrease(event.getId(), event.getAmount());
    }

}
