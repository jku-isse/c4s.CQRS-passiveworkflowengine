package at.jku.isse.passiveprocessengine.frontend.ui;


import java.util.Collection;

import com.vaadin.flow.component.UI;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;

public interface IFrontendPusher {

    void update(ProcessInstance wfi);
    
    void updateAll();

    void update(Collection<ProcessInstance> wfis);
    
    void remove(String wfiId);

    void add(int id, UI ui, MainView view);

    void remove(int id);
    
    void requestUpdate(UI ui, MainView view);
}
