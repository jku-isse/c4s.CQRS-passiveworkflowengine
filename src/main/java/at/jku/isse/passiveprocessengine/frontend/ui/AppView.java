package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;

import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.monitoring.GlobalProgressView;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Push
public class AppView extends AppLayout{

	private UIConfig uiconf;
	private final Tabs tabs;
	private H1 title;
	
	public AppView(SecurityService securityService, UIConfig uiconf) {
		this.uiconf = uiconf;
		DrawerToggle toggle = new DrawerToggle();

	    title = new H1();
	    title.getStyle()
	      .set("font-size", "var(--lumo-font-size-l)")
	      .set("margin", "0");
	    Icon logoutIcon = VaadinIcon.SIGN_OUT.create();
	    logoutIcon.getStyle()
	      .set("box-sizing", "border-box")
	      .set("margin-inline-end", "var(--lumo-space-m)")
	      .set("margin-inline-start", "var(--lumo-space-xs)")
	      .set("padding", "var(--lumo-space-xs)");
	    Button logout = new Button("Log out", logoutIcon, e -> securityService.logout()); 

	    HorizontalLayout header = new HorizontalLayout(toggle, title, logout); 
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title); 
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");
	    	   
	    tabs = getTabs();

	    addToDrawer(tabs);
	    addToNavbar(header);
	}
	
	private Tabs getTabs() {
	    final Tabs tabs = new Tabs();
	    tabs.add(
	      createTab(VaadinIcon.DASHBOARD, "Process Instances", MainView.class),
	      createTab(VaadinIcon.RECORDS, "Process Definitions", DefinitionView.class),	      
	      createTab(VaadinIcon.COMPILE, "Connector Progress", GlobalProgressView.class),
	      createTab(VaadinIcon.GAMEPAD, "Rule Playground", ARLPlaygroundView.class),
	      createTab(VaadinIcon.SPECIALIST, "Artifact/Instance Inspector", InstanceView.class)	      
	    );
	    if (uiconf.isBlocklyEditorEnabled()) {
	    	tabs.add(
	    			createTab(VaadinIcon.EDIT, "Local Process Editor", BlocklyEditorView.class),	    	
	    			createTab(VaadinIcon.AUTOMATION, "Process Deployment Result", DeployResultView.class)
		    );		      
	    } 
	    if(uiconf.isStagesEnabled()) {
		     tabs.add(createTab(VaadinIcon.AUTOMATION, "Stages Transformation Result", StagesTransformationResultView.class));
	    }
	    
	    if (uiconf.isExperimentModeEnabled()) {
	    	tabs.add(  createTab(VaadinIcon.FLASK, "Experiment Overview", ExperimentParticipantView.class)	   );
	    }
	    tabs.setOrientation(Tabs.Orientation.VERTICAL);
	    return tabs;
	  }
	

	  private Tab createTab(VaadinIcon viewIcon, String viewName,  Class<? extends Component> clazz) {
	    Icon icon = viewIcon.create();
	    icon.getStyle()
	      .set("box-sizing", "border-box")
	      .set("margin-inline-end", "var(--lumo-space-m)")
	      .set("margin-inline-start", "var(--lumo-space-xs)")
	      .set("padding", "var(--lumo-space-xs)");

	    RouterLink link = new RouterLink();
	    link.add(icon, new Span(viewName));
	    
	    link.setRoute(clazz);
	    link.setTabIndex(-1);

	    Tab tab =  new Tab(link);
	    ComponentUtil.setData(tab, Class.class, clazz);
	    return tab;
	  }
	  
	  @Override
	    protected void afterNavigation() {
	        super.afterNavigation();

	        // Select the tab corresponding to currently shown view
	        getTabForComponent(getContent()).ifPresent(tabs::setSelectedTab);

	        // Set the view title in the header
	        title.setText(getCurrentPageTitle());
	    }

	    private Optional<Tab> getTabForComponent(Component component) {
	        return tabs.getChildren()
	                .filter(tab -> ComponentUtil.getData(tab, Class.class)
	                        .equals(component.getClass()))
	                .findFirst().map(Tab.class::cast);
	    }

	    private String getCurrentPageTitle() {
	        return getContent().getClass().getAnnotation(PageTitle.class).value();
	    }
}
