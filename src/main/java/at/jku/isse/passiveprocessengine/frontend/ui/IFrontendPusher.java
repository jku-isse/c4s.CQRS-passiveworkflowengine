package at.jku.isse.passiveprocessengine.frontend.ui;


import java.util.Collection;

import com.vaadin.flow.component.UI;

import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;


public interface IFrontendPusher {

    void update(ProcessInstance wfi);
    
    void updateAll();

    void update(Collection<ProcessInstance> wfis);
    
    void remove(String wfiId);

    void add(String id, UI ui, MainView view, String filterToProcessId);
    
    void removeView(String id);
    
    void requestUpdate(String id, UI ui, MainView view);

}
