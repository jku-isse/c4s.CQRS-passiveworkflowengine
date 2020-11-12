package impactassessment.ui;

import com.vaadin.flow.component.UI;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("singleton")
@NoArgsConstructor
public class FrontendPusher {

    private @Setter UI ui;
    private @Setter MainView view;

    public void update(List<WorkflowInstanceWrapper> state) {
        if (ui != null && view != null) {
            ui.access(() -> view.getGrids().stream()
                    .filter(com.vaadin.flow.component.Component::isVisible)
                    .forEach(grid -> grid.updateTreeGrid(state)));
        }
    }

    public void updateFetchTimer() {
        if (ui != null && view != null) {
            ui.access(() -> {
                view.getTimer().reset();
                view.getTimer().start();
            });
        }
    }
}
