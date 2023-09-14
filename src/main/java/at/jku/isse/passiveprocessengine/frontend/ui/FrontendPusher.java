package at.jku.isse.passiveprocessengine.frontend.ui;

import com.vaadin.flow.component.UI;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private HashMap<String,ProcessInstance> processes = new HashMap<>();
    

    @Override
    public void add(int id, UI ui, MainView view) {
        views.put(id, new MainViewState(ui, view, SecurityContextHolder.getContext().getAuthentication()));
    }

    @Override
    public void remove(int id) {
        views.remove(id);
    }
    
    

    @Override
    public void update(ProcessInstance wfi) {
    	this.update(List.of(wfi));
    }

    @Override
    public void remove(String wfiId) {
    	processes.remove(wfiId);
    	Authentication currAuth = SecurityContextHolder.getContext().getAuthentication();
    	for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
                if (ui != null && view != null) {
                    ui.access(() -> {
                    	SecurityContextHolder.getContext().setAuthentication(state.getAuth());
                    	view.getGrids().stream()                  
                            .filter(com.vaadin.flow.component.Component::isVisible)
                            .forEach(grid -> grid.removeWorkflow(wfiId));
                    });
                }
        }
    	SecurityContextHolder.getContext().setAuthentication(currAuth);
    }

	@Override
	public void update(Collection<ProcessInstance> wfis) {
		if (wfis.isEmpty()) return;
		wfis.forEach(wfi -> processes.put(wfi.getName(), wfi));
		Authentication currAuth = SecurityContextHolder.getContext().getAuthentication();
        for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
            if (ui != null && view != null) {
                ui.access(() -> { 
                	SecurityContextHolder.getContext().setAuthentication(state.getAuth());
                	view.getGrids().stream()
                        .filter(com.vaadin.flow.component.Component::isVisible)
                        .forEach(grid -> grid.updateTreeGrid(wfis));
                        });
            }
        }
        SecurityContextHolder.getContext().setAuthentication(currAuth);
	}

	@Override
	public void updateAll() {
		this.update(processes.values());
	}

	@Override
	public void requestUpdate(UI ui, MainView view) {
		ui.access(() -> view.getGrids().stream()
                .filter(com.vaadin.flow.component.Component::isVisible)
                .forEach(grid -> grid.updateTreeGrid(processes.values())));
	}

}
