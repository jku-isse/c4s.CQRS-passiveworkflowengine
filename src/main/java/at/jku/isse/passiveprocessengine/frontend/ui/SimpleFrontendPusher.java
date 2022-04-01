package at.jku.isse.passiveprocessengine.frontend.ui;

import com.vaadin.flow.component.UI;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;

import java.util.Collection;

public class SimpleFrontendPusher implements IFrontendPusher {

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
		// TODO Auto-generated method stub
		
	}
}
