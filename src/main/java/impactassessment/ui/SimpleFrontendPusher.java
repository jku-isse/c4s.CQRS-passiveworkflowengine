package impactassessment.ui;

import com.vaadin.flow.component.UI;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;

import java.util.List;

public class SimpleFrontendPusher implements IFrontendPusher {

    @Override
    public void update(List<WorkflowInstanceWrapper> state) {
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
