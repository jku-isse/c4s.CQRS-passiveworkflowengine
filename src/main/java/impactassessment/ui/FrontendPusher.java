package impactassessment.ui;

import com.vaadin.flow.component.UI;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import passiveprocessengine.instance.WorkflowInstance;

import java.time.Duration;
import java.time.Instant;

@Component
@Scope("singleton")
@NoArgsConstructor
@Slf4j
public class FrontendPusher implements IFrontendPusher {

    private @Setter UI ui;
    private @Setter MainView view;

    private Instant lastUpdate = Instant.now();
    private final boolean PERFORMANCE_MODE = false;

    @Override
    public void update(WorkflowInstance state) {
        if (!PERFORMANCE_MODE || Duration.between(lastUpdate, Instant.now()).getNano() > 200000000) {
            log.debug("update frontend");
            lastUpdate = Instant.now();
            if (ui != null && view != null) {
                ui.access(() -> view.getGrids().stream()
                        .filter(com.vaadin.flow.component.Component::isVisible)
                        .forEach(grid -> grid.updateTreeGrid(state)));
            }
        }
    }

//    public void updateFetchTimer() {
//        if (ui != null && view != null) {
//            ui.access(() -> {
//                view.getTimer().reset();
//                view.getTimer().start();
//            });
//        }
//    }
}
