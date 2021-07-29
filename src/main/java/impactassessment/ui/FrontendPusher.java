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
import java.util.*;

@Component
@Scope("singleton")
@NoArgsConstructor
@Slf4j
public class FrontendPusher implements IFrontendPusher {

    private Map<Integer, MainViewState> views;

    private Instant lastUpdate = Instant.now();
    private final boolean PERFORMANCE_MODE = false;

    @Override
    public void add(int id, UI ui, MainView view) {
        if (views == null)
            views = new HashMap<>();
        views.put(id, new MainViewState(ui, view));
    }

    @Override
    public void remove(int id) {
        views.remove(id);
    }

    @Override
    public void update(WorkflowInstance wfi) {
        for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
            if (!PERFORMANCE_MODE || Duration.between(lastUpdate, Instant.now()).getNano() > 200000000) {
                log.debug("update frontend");
                lastUpdate = Instant.now();
                if (ui != null && view != null) {
                    ui.access(() -> view.getGrids().stream()
                            .filter(com.vaadin.flow.component.Component::isVisible)
                            .forEach(grid -> grid.updateTreeGrid(wfi)));
                }
            }
        }
    }

    @Override
    public void remove(String wfiId) {
        for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
            if (!PERFORMANCE_MODE || Duration.between(lastUpdate, Instant.now()).getNano() > 200000000) {
                log.debug("update frontend");
                lastUpdate = Instant.now();
                if (ui != null && view != null) {
                    ui.access(() -> view.getGrids().stream()
                            .filter(com.vaadin.flow.component.Component::isVisible)
                            .forEach(grid -> grid.removeWorkflow(wfiId)));
                }
            }
        }
    }

	@Override
	public void update(Collection<WorkflowInstance> wfis) {
        for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
            if (ui != null && view != null) {
                ui.access(() -> view.getGrids().stream()
                        .filter(com.vaadin.flow.component.Component::isVisible)
                        .forEach(grid -> grid.updateTreeGrid(wfis)));
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
