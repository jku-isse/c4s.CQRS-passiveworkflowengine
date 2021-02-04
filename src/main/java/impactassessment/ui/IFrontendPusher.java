package impactassessment.ui;

import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;

import java.util.List;

public interface IFrontendPusher {
    void update(List<WorkflowInstanceWrapper> state);

    void setUi(com.vaadin.flow.component.UI ui);

    void setView(MainView view);
}
