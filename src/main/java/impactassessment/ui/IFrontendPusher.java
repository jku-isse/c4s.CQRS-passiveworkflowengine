package impactassessment.ui;


import passiveprocessengine.instance.WorkflowInstance;

public interface IFrontendPusher {

    void update(WorkflowInstance wfi);

    void setUi(com.vaadin.flow.component.UI ui);

    void setView(MainView view);
}
