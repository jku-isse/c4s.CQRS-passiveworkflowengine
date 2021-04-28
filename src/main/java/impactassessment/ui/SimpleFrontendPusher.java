package impactassessment.ui;

import com.vaadin.flow.component.UI;
import passiveprocessengine.instance.WorkflowInstance;

import java.util.List;

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
    public void setUi(UI ui) {
        // no op
    }

    @Override
    public void setView(MainView view) {
        // no op
    }
}
