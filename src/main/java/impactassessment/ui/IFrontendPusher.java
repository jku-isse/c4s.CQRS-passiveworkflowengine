package impactassessment.ui;


import java.util.Collection;

import com.vaadin.flow.component.UI;
import passiveprocessengine.instance.WorkflowInstance;

public interface IFrontendPusher {

    void update(WorkflowInstance wfi);

    void update(Collection<WorkflowInstance> wfis);
    
    void remove(String wfiId);

    void add(int id, UI ui, MainView view);

    void remove(int id);
}
