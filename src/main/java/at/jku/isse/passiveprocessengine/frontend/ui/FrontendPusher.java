package at.jku.isse.passiveprocessengine.frontend.ui;

import com.vaadin.flow.component.UI;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
@Scope("singleton")
@NoArgsConstructor
@Slf4j
public class FrontendPusher implements IFrontendPusher {

    private Map<Integer, MainViewState> views = new HashMap<>();

    private Instant lastUpdate = Instant.now();
    private HashMap<String,ProcessInstance> processes = new HashMap<>();
    

    @Override
    public void add(int id, UI ui, MainView view) {
        views.put(id, new MainViewState(ui, view));
    }

    @Override
    public void remove(int id) {
        views.remove(id);
    }
    
    

    @Override
    public void update(ProcessInstance wfi) {
    	processes.put(wfi.getName(), wfi);
        for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
        //    if (Duration.between(lastUpdate, Instant.now()).getNano() > 200000000) {
                log.debug("update frontend");
                lastUpdate = Instant.now();
                if (ui != null && view != null) {
                    ui.access(() -> view.getGrids().stream()
                            .filter(com.vaadin.flow.component.Component::isVisible)
                            .forEach(grid -> grid.updateTreeGrid(wfi)));
                }
       //     }
        }
    }

    @Override
    public void remove(String wfiId) {
        for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
         //   if (Duration.between(lastUpdate, Instant.now()).getNano() > 200000000) {
                log.debug("update frontend");
                lastUpdate = Instant.now();
                if (ui != null && view != null) {
                    ui.access(() -> view.getGrids().stream()
                            .filter(com.vaadin.flow.component.Component::isVisible)
                            .forEach(grid -> grid.removeWorkflow(wfiId)));
                }
        //    }
        }
        processes.remove(wfiId);
    }

	@Override
	public void update(Collection<ProcessInstance> wfis) {
		if (wfis.isEmpty()) return;
		wfis.forEach(wfi -> processes.put(wfi.getName(), wfi));
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

	@Override
	public void updateAll() {
		for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
            if (ui != null && view != null) {
                requestUpdate(ui, view);
            }
        }
	}

	@Override
	public void requestUpdate(UI ui, MainView view) {
		ui.access(() -> view.getGrids().stream()
                .filter(com.vaadin.flow.component.Component::isVisible)
                .forEach(grid -> grid.updateTreeGrid(processes.values())));
	}

}
