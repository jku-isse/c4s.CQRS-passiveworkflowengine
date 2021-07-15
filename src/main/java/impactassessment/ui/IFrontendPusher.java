package impactassessment.ui;


import java.util.Collection;

import passiveprocessengine.instance.WorkflowInstance;

public interface IFrontendPusher {

    void update(WorkflowInstance wfi);

    void update(Collection<WorkflowInstance> wfis);
    
    void remove(String wfiId);

    void setUi(com.vaadin.flow.component.UI ui);

    void setView(MainView view);
}
