package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.Collection;

import com.vaadin.flow.component.UI;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;

public class NoOpFrontendPusher implements IFrontendPusher {

    @Override
    public void update(ProcessInstance wfi) {
        // no op
    }

    @Override
    public void remove(String wfiId) {
        // no op
    }

    @Override
    public void add(int id, UI ui, MainView view) {
        // no op
    }

    @Override
    public void remove(int id) {
        // no op
    }

    @Override
	public void update(Collection<ProcessInstance> wfis) {
		// noop		
	}

	@Override
	public void updateAll() {
		// no op
		
	}

	@Override
	public void requestUpdate(UI ui, MainView view) {
		// no op
		
	}
}
