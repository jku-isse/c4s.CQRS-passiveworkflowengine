package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;

import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Scope("singleton")
@NoArgsConstructor
@Slf4j
public class FrontendPusher implements IFrontendPusher {

    private Map<Integer, MainViewState> views = new ConcurrentHashMap<>();

    private Map<String,ProcessInstance> processes = new ConcurrentHashMap<>();
    

    
    @Override
    public void add(int id, UI ui, MainView view, String filterToProcessId) {
        views.put(id, new MainViewState(ui, view, SecurityContextHolder.getContext().getAuthentication(), filterToProcessId));
        
        log.info(String.format("Registering %s for process %s", id, filterToProcessId));
    }

    @Override
    public void remove(int id) {
        var view = views.remove(id);
        if (view != null)
        	log.info(String.format("Unregistering %s for process %s", id, view.getProcessIdFilter()));        
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
                if (ui != null && view != null && doesViewSubscribeToProcess(wfiId, state)) {
                	log.info(String.format("About to notify %s for process %s removal", ui.getUIId(), wfiId));
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
        for (ProcessInstance wfi : wfis) {
		for (MainViewState state : views.values()) {
            UI ui = state.getUi();
            MainView view = state.getView();
            if (ui != null && view != null && doesViewSubscribeToProcess(wfi.getInstance().getId(), state)) {
            	log.info(String.format("About to notify %s for process %s update", ui.getUIId(), wfi.getInstance().getId()));
                ui.access(() -> { 
                	SecurityContextHolder.getContext().setAuthentication(state.getAuth());
                	view.getGrid().updateTreeGrid(List.of(wfi));
                        });
            }
        }
        }
        SecurityContextHolder.getContext().setAuthentication(currAuth);
	}
	
	private boolean doesViewSubscribeToProcess(String processId, MainViewState view) {
		if (view == null) return true;
		if (view.getProcessIdFilter() == null || view.getProcessIdFilter().length() == 0) return true;
		if (processId == null) return true;				
		var matches = processId.equals(view.getProcessIdFilter());
		return matches;
	}

	@Override
	public void updateAll() {
		this.update(processes.values());
	}

	@Override
	public void requestUpdate(UI ui, MainView view) {
		var state = views.get(ui.getUIId());
		ui.access(() -> view.getGrid().updateTreeGrid(
				processes.values().stream()
					.filter(wfi -> doesViewSubscribeToProcess(wfi.getInstance().getId(), state)) 
					.toList()
				));
	}



}
