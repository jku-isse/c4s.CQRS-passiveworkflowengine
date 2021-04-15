package impactassessment.ui;


import passiveprocessengine.instance.WorkflowInstance;

import java.util.List;

public interface IFrontendPusher {
    void update(List<WorkflowInstance> state);

    void setUi(com.vaadin.flow.component.UI ui);

    void setView(MainView view);
}
