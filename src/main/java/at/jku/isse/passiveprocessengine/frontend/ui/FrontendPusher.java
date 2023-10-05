package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                    	view.getGrid().removeWorkflow(wfiId);
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
                	view.getGrid().updateTreeGrid(wfis);
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
		ui.access(() -> view.getGrid().updateTreeGrid(processes.values()));
	}

}
