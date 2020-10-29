package impactassessment.ui;

import com.vaadin.flow.component.UI;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("singleton")
@NoArgsConstructor
public class FrontendPusher {

    private UI ui;
    private MainView view;

    void setUi(UI ui) {
        this.ui = ui;
    }

    void setView(MainView view) {
        this.view = view;
    }

    public void update(List<WorkflowInstanceWrapper> state) {
        ui.access(() -> view.getGrids().stream()
                .filter(com.vaadin.flow.component.Component::isVisible)
                .forEach(grid -> grid.updateTreeGrid(state)));
    }
}
