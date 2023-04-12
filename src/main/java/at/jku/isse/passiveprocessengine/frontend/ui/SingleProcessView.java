package at.jku.isse.passiveprocessengine.frontend.ui;

import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppFooter;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.StepLifecycleStateMapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;


@Slf4j
@Route("processinstance")
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Instance")
@UIScope
//@SpringComponent
public class SingleProcessView extends VerticalLayout implements HasUrlParameter<String> {
     
	@Autowired
    protected RequestDelegate commandGateway;

    private Id id = null;
   

    @Override
    public void setParameter(BeforeEvent beforeEvent, String s) {
        // example link: http://localhost:8080/processinstance/show?id=[DESIGNSPACEID]
        Location location = beforeEvent.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();

        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        String strid = parametersMap.getOrDefault("id", List.of("")).get(0);
        try {
        	long lId = Long.parseLong(strid);
        	id = Id.of(lId);
        	content();
        } catch(Exception e) {
        	
        	log.warn("Parameter id cannot be parsed to long");
        }
        
    }
    
    public SingleProcessView(RequestDelegate commandGateway) {
    	this.commandGateway = commandGateway;
        setSizeFull();
        setMargin(false);
        setPadding(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setClassName("header-theme");
        header.setMargin(false);
        header.setPadding(true);
        header.setSizeFull();
        header.setHeight("6%");
        HorizontalLayout firstPart = new HorizontalLayout();
        firstPart.setClassName("header-theme");
        firstPart.setMargin(false);
        firstPart.setPadding(true);
        firstPart.setSizeFull();
        firstPart.add(new Icon(VaadinIcon.CLUSTER), new Label(""), new Text("Process Instance"));
       
        header.add(firstPart/*, shutdown*/);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout footer = new AppFooter(commandGateway.getUIConfig());

        add(
                header,
                main(),
                footer
        );
    }

    private Component main() {
        HorizontalLayout main = new HorizontalLayout();
        main.setClassName("layout-style");
        main.setHeight("91%");
        main.add( content());
        return main;
    }

    VerticalLayout pageContent = new VerticalLayout();
    
    private Component content() {
       // Tab tab1 = new Tab("Current State");
        VerticalLayout cur = statePanel();
        cur.setHeight("100%");

        Div pages = new Div(cur); //, snap, split
        pages.setHeight("97%");
        pages.setWidthFull();

        pageContent.removeAll();
        pageContent.setClassName("layout-style");
        pageContent.add(/*tabs,*/ pages);
        return pageContent;
    }
  


    private VerticalLayout statePanel() {
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.setWidthFull();
        layout.setFlexGrow(0);
        if (commandGateway != null && commandGateway.getWorkspace() != null && id != null) {
        	Element el = commandGateway.getWorkspace().findElement(id);
        	if (el instanceof Instance) {
        		ProcessInstance pi = WrapperCache.getWrappedInstance(ProcessInstance.class, (Instance)el);
    			Component process = createProcessComponent(pi);
            	Component detail = createStepDetails(pi);

            	SplitLayout splitLayout = new SplitLayout(process, detail);            	
            	splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
            	layout.add(splitLayout);

        	}
        	//TODO: enable automatic reloading upon process change
        	        	        	        	
        }
        return layout;
    }
    
    
    private Component createProcessComponent(ProcessInstance pi) {
    	FullProcessInstanceGrid pGrid = new FullProcessInstanceGrid();
    	pGrid.updateProcessGrid(pi);
    	//TODO: attach selection listner to update DetailedContent
    	return pGrid;
    }
    
    private Component createStepDetails(ProcessStep step) {
    	// TODO make this a reusable component for the main page to reuse
    	VerticalLayout l = new VerticalLayout();
        l.setClassName("scrollable");
        Paragraph p = new Paragraph("Process Step ID:  " +step.getId());
        p.setClassName("info-header");
        l.add(p);
        H3 h3= new H3(step.getName());
        h3.setClassName("info-header");
        l.add(h3);
        if(step.getDefinition().getHtml_url()!=null)
        {
        	Anchor a =new Anchor(step.getDefinition().getHtml_url(),step.getDefinition().getHtml_url());
        	a.setClassName("info-header");
        	a.setTarget("_blank");
        	l.add(a);
        }
        if(step.getDefinition().getDescription()!=null)
        {
        	Html h=new Html("<span>"+step.getDefinition().getDescription()+"</span>");
        	l.add(h);
        }
        if (step.getActualLifecycleState() != null) {
         //TODO for now just actual state
        	//l.add(new Paragraph(String.format("Lifecycle State: %s (Actual) - %s (Expected)", wft.getActualLifecycleState().name(), wft.getExpectedLifecycleState().name())));
        	l.add(new Paragraph("Step State: "+StepLifecycleStateMapper.translateState(step.getActualLifecycleState())));
        }
        return l;
    }
   
}
