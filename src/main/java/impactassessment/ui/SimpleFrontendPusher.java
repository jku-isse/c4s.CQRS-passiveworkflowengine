package impactassessment.ui;

import com.vaadin.flow.component.UI;
import passiveprocessengine.instance.WorkflowInstance;

import java.util.Collection;

public class SimpleFrontendPusher implements IFrontendPusher {

    @Override
    public void update(WorkflowInstance wfi) {
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
	public void update(Collection<WorkflowInstance> wfis) {
		// noop		
	}
}
