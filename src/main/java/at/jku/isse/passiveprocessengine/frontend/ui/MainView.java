package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.componentfactory.ToggleButton;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.controlflow.ControlEventEngine;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppFooter;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppHeader;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RefreshableComponent;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route("home")
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Dashboard")
@UIScope
//@SpringComponent
public class MainView extends VerticalLayout implements HasUrlParameter<String>, RefreshableComponent  {

   // private boolean devMode = false;
   // public static final boolean anonymMode = false;
    	
    @Autowired
    private RequestDelegate commandGateway;
    @Autowired
    private IFrontendPusher pusher;
    private WorkflowTreeGrid grid;
    
    private @Getter List<WorkflowTreeGrid> grids = new ArrayList<>();

   
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        pusher.add(attachEvent.getUI().getUIId(), attachEvent.getUI(), this);
        //grids.stream()
        //        .filter(com.vaadin.flow.component.Component::isVisible)
        //        .forEach(this::refresh);
        if (grids.stream().anyMatch(com.vaadin.flow.component.Component::isVisible))
        	this.refresh(null);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        pusher.remove(detachEvent.getUI().getUIId());
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
        // example link: http://localhost:8080/home/?key=DEMO-9&value=Task
        Location location = beforeEvent.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();

        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        String id = parametersMap.getOrDefault("id", List.of("")).get(0);
        //String key = parametersMap.getOrDefault("key", List.of("")).get(0);
        //String value = parametersMap.getOrDefault("value", List.of("")).get(0);
        String name = parametersMap.getOrDefault("name", List.of("")).get(0);
        String focus = parametersMap.getOrDefault("focus", List.of("")).get(0);
        initAccordion(id, name, focus);
    }

//    @Override
//    public void configurePage(InitialPageSettings settings) {
//        HashMap<String, String> attributes = new HashMap<>();
//        attributes.put("rel", "shortcut icon");
//        settings.addLink("icons/favicon.ico", attributes);
//    }

    public MainView(RequestDelegate reqDel, IFrontendPusher pusher, SecurityService securityService) {
    	 this.commandGateway = reqDel;
    	 this.pusher = pusher;
    	setSizeFull();
        setMargin(false);
        setPadding(false);
        
        AppHeader header = new AppHeader("Process Dashboard", securityService, commandGateway.getUIConfig());  
        AppFooter footer = new AppFooter(commandGateway.getUIConfig()); 
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
        main.add(menu(), content());
        return main;
    }

    VerticalLayout pageContent = new VerticalLayout();
    private Component content() {
    	initAccordion();
    	refreshContent();
    	return pageContent;
    }
  
	@Override
	public void refreshContent() {		
        VerticalLayout cur = statePanel();
        cur.setHeight("100%");

        Div pages = new Div(cur); //, snap, split
        pages.setHeight("97%");
        pages.setWidthFull();

        pageContent.removeAll();
        pageContent.setClassName("layout-style");
        pageContent.add(/*tabs,*/ pages);
	}

    private Component menu() {
        VerticalLayout menu = new VerticalLayout();
        menu.addClassName("light-theme");
        menu.addClassName("scrollable");
        menu.setPadding(true);
        menu.setMargin(false);
        menu.setWidth("35%");
        menu.setFlexGrow(0);


        initAccordion();
        menu.add(new H2("Controls"), accordion);

        return menu;
    }

    private Accordion accordion = new Accordion();

    private void initAccordion() {
        initAccordion("", "", "");
    }

    private void initAccordion(String id, String name, String focus) {
        accordion.getChildren().forEach(c -> accordion.remove(c));
        accordion.add("Create Process Instance", importArtifact());
    //    accordion.add("Fetch Updates", updates());
        if (commandGateway != null && !commandGateway.getUIConfig().isAnonymized()) {
        	accordion.add("Fetch Artifact", fetchArtifact(commandGateway.getArtifactResolver()));
           // accordion.add("Dump Designspace", dumpDesignSpace(commandGateway));
        }

        accordion.add("Filter", filterTable(id, name, focus));
    //    if (devMode) accordion.add("Backend Queries", backend());
        accordion.close();
        accordion.open(0);
        accordion.setWidthFull();
    }

    private Component filterTable(String id, String n, String focus) {
        Paragraph p1 = new Paragraph("Filter on process properties:");
//        TextField key = new TextField();
//        key.setLabel("KEY");
//        key.setValue(k);
//        TextField val = new TextField();
//        val.setLabel("VALUE");
//        val.setValue(v);
//        val.setValueChangeMode(ValueChangeMode.EAGER);
//        key.setValueChangeMode(ValueChangeMode.EAGER);
        HorizontalLayout line = new HorizontalLayout();
        line.setWidthFull();
//        line.add(key, val);
        Paragraph p2 = new Paragraph("Filter on process name:");
        TextField nameField = new TextField();
        //nameField.setLabel("NAME");
        nameField.setValue(n);
        nameField.setValueChangeMode(ValueChangeMode.EAGER);

        Paragraph p3 = new Paragraph("Filter on process id:");
        TextField idField = new TextField();
        //idField.setLabel("ID");
        idField.setValue(id);
        idField.setValueChangeMode(ValueChangeMode.EAGER);
        
//        val.addValueChangeListener(e -> {
//            Map<String, String> filter = new HashMap<>();
//            filter.put(key.getValue(), val.getValue());
//            grids.forEach(grid -> grid.setFilters(filter, name.getValue()));
//        });
//        key.addValueChangeListener(e -> {
//            Map<String, String> filter = new HashMap<>();
//            filter.put(key.getValue(), val.getValue());
//            grids.forEach(grid -> grid.setFilters(filter, name.getValue()));
//        });
        nameField.addValueChangeListener(e -> {
           // Map<String, String> filter = new HashMap<>();
           // filter.put(key.getValue(), val.getValue());
            grids.forEach(grid -> grid.setFilters(idField.getValue(), nameField.getValue(), focus));
        });
        idField.addValueChangeListener(e -> {
            // Map<String, String> filter = new HashMap<>();
            // filter.put(key.getValue(), val.getValue());
             grids.forEach(grid -> grid.setFilters(idField.getValue(), nameField.getValue(), focus));
         });
        
        
        grids.forEach(grid -> grid.setFilters(idField.getValue(), nameField.getValue(), focus));

        Button clearFilters = new Button("Clear all Filters", e -> {            
            nameField.setValue("");
            idField.setValue("");
        });
        clearFilters.addThemeVariants(ButtonVariant.LUMO_ERROR);

        return new VerticalLayout(p2, nameField, p3, idField, line, clearFilters);
    }

    private Component currentStateControls(WorkflowTreeGrid grid) {
        HorizontalLayout controlButtonLayout = new HorizontalLayout();
        controlButtonLayout.setMargin(false);
        controlButtonLayout.setPadding(false);
        controlButtonLayout.setWidthFull();

        Button getState = new Button("Refresh State");
        getState.addClickListener(evt -> refresh(grid));
        getState.getElement().setProperty("title", "The whole state is automatically updated on events that change the state. You can refresh the state manually with this button if necessary!");

        controlButtonLayout.add(getState);
        return controlButtonLayout;
    }

    private void refresh(WorkflowTreeGrid grid) {
    	if (commandGateway != null) {
    		int count = commandGateway.resetAndUpdate();
    		if (count <= 0) // otherwise there is an update called from the FrontendPusher
    			pusher.requestUpdate(getUI().get(), this);
    	} else {
    		Notification.show("Toogle Refresher to trigger backend update!");
    		pusher.requestUpdate(getUI().get(), this);
    	}
    }
    



    private Component importArtifact() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setWidth("90%");
        List<String> wfdKeys = new LinkedList<>();
        // Process Definition
        if (commandGateway != null) {
        	wfdKeys.addAll(commandGateway.getRegistry().getAllDefinitionIDs(true));
        	if (grid != null)
        		grid.injectRequestDelegate(commandGateway);
        }
        Collections.sort(wfdKeys);	
        RadioButtonGroup<String> processDefinition = new RadioButtonGroup<>();
        processDefinition.setItems(wfdKeys);
        processDefinition.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);


        Button loadDefinitions = new Button("Fetch Available Definitions", e -> {
            initAccordion();
        });

        // Source
        VerticalLayout source = new VerticalLayout();
        source.setMargin(false);
        source.setPadding(false);
        source.setWidthFull();
        source.add(new Paragraph("select a process definition"));
        Map<String, ComboBox<String>> role2IdType = new HashMap<>();
        
        processDefinition.addValueChangeListener( e -> {
            ProcessDefinition wfdContainer = commandGateway.getRegistry().getProcessDefinition(e.getValue(), true).get(); // we fetched the ids earlier, should exist here
            source.removeAll();
            role2IdType.clear();
            for (Map.Entry<String, InstanceType> entry : wfdContainer.getExpectedInput().entrySet()) {
                InstanceType artT = entry.getValue();
                List<String> idTypes = commandGateway.getArtifactResolver().getIdentifierTypesForInstanceType(artT);
                String role = entry.getKey();
                TextField tf = new TextField();
                tf.setWidthFull();
                tf.setLabel(role);                
                tf.setHelperText(artT.name());
                source.add(tf);
                ComboBox<String> idTypeBox = new ComboBox<>("Identifier Type");
                role2IdType.put(role, idTypeBox);
                idTypeBox.setItems(idTypes);
                idTypeBox.setValue(idTypes.get(0));
                idTypeBox.setHelperText(role);
                idTypeBox.setAllowCustomValue(false);
                source.add(idTypeBox);
            }
            if (wfdContainer.getExpectedInput().size() == 0) {
                source.add(new Paragraph("no inputs expected"));
            }
        });

        Button importOrUpdateArtifactButton = new Button("Create", evt -> {
        	if (processDefinition.getValue() == null) {
        		Notification.show("Select a Process Definition first!");
        	} else {

        		// collect all input IDs
        		Map<String, ArtifactIdentifier> inputs = new LinkedHashMap<>();
        		AtomicInteger count = new AtomicInteger();
        		source.getChildren()
        		.filter(child -> child instanceof TextField)
        		.map(child -> {
        			count.getAndIncrement();
        			return (TextField) child;
        		})
        		.filter(tf -> !tf.getValue().equals(""))
        		.filter(tf -> !tf.getLabel().equals(""))
        		.forEach(tf -> {
        			String role = tf.getLabel().trim();
        			String artId = tf.getValue().trim();
        			String artType = tf.getHelperText().trim();
        			ComboBox<String> idTypeSel = role2IdType.get(role);
        			String idType = idTypeSel.getOptionalValue().orElse(artType);                            	                            	
        			ArtifactIdentifier ai = new ArtifactIdentifier(artId, artType, idType);
        			inputs.put(role,ai);
        		});
        		// send command
        		if (count.get() == inputs.size()) {
        			// FIXME: hack for ensuring only input that user is allowed to access can be used to instantiate a process:
        			String artId = inputs.values().iterator().next().getId();
        			if (!commandGateway.doAllowProcessInstantiation(artId)) {
        				Notification.show("You are not authorized to access the artifact used as process input - unable to instantiate process.");        			
        			} else {
        				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        				
        				String nextAllowedProc = commandGateway.isAllowedAsNextProc(processDefinition.getValue(), auth != null ? auth.getName() : null);
        				if (!nextAllowedProc.equalsIgnoreCase(processDefinition.getValue())) {
        					Notification.show("You are not authorized to instantiate this process now: "+nextAllowedProc);
        				} else {

        					//inputs.keySet().stream().map(ai -> ai.get)
        					String id = inputs.values().stream().map(ai -> ai.getId()).collect(Collectors.joining(""))+processDefinition.getValue(); //getNewId()
        					Notification.show("Process Instantiation might take some time. UI will be updated automatically upon success.");
        					new Thread(() -> { 
        						try {
        							ProcessInstance pi = commandGateway.instantiateProcess(id, inputs, processDefinition.getValue());
        							if (auth != null && auth.getName() != null) {
        								pi.getInstance().addOwner(new User(auth.getName()));
        							}
        							commandGateway.getMonitor().processCreated(pi, auth != null ? auth.getName() : null);
        							this.getUI().get().access(() ->Notification.show("Success"));
        						} catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
        							log.error("CommandExecutionException: " + e.getMessage());
        							e.printStackTrace();
        							this.getUI().get().access(() ->Notification.show("Creation failed! \r\n"+e.getMessage()));
        						}
        					} ).start();
        				}
        			}
        		} else {
        			Notification.show("Make sure to fill out all required artifact IDs!");
        		}

        	}
        });
        importOrUpdateArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);

        if (wfdKeys.isEmpty()) {
            Paragraph par = new Paragraph("Reload:");
            layout.add(
                    new H4("1. Select Process Definition"),
                    par,
                    loadDefinitions);
        } else {
            layout.add(
                    new H4("1. Select Process Definition"),
                    processDefinition,
                    loadDefinitions);
        }
//        if (devMode) layout.add(
//                upload,
//                addDefinition);
        layout.add(
                new H4("2. Enter Artifact ID(s)"),
                source,
                importOrUpdateArtifactButton);
        return layout;
    }
    
    private Component fetchArtifact(ArtifactResolver artRes) {
    	VerticalLayout layout = new VerticalLayout();
        Paragraph p1 = new Paragraph("Fetch Artifact:");
        TextField artIdField = new TextField();
    	ComboBox<InstanceType> artTypeBox = new ComboBox<>("Instance Type");
    	Set<InstanceType> instTypes = commandGateway.getArtifactResolver().getAvailableInstanceTypes();
    	//List<String> instTypes = List.of("git_issue", "azure_workitem", "jira_core_artifact", "jama_item");
    	artTypeBox.setItems(instTypes);
    	artTypeBox.setItemLabelGenerator(new ItemLabelGenerator<InstanceType>() {
			@Override
			public String apply(InstanceType item) {
				return item.name();
			}});
    	ComboBox<String> idTypeBox = new ComboBox<>("Identifier Type");    	  
    	
		
    	
    	artTypeBox.addValueChangeListener(e-> {
    		InstanceType artT = artTypeBox.getOptionalValue().get();
    		List<String> idTypes = commandGateway.getArtifactResolver().getIdentifierTypesForInstanceType(artT);
    		idTypeBox.setItems(idTypes);
    		idTypeBox.setValue(idTypes.get(0));
    	});
    	
    	Button importArtifactButton = new Button("Fetch", evt -> {
            
                try {
                	if (artTypeBox.getOptionalValue().isEmpty())
            			Notification.show("Make sure to select an Artifact Type!");
                	else if (artIdField.getValue().length() < 1)
                		Notification.show("Make sure to provide an identifier!");
                	else {
                		String idValue = artIdField.getValue().trim();
                		String artType = artTypeBox.getOptionalValue().get().name();
                		String idType = idTypeBox.getOptionalValue().get();
                		
                		ArtifactIdentifier ai = new ArtifactIdentifier(idValue, artType, idType);
                		boolean forceRefetch = true;
                		Instance inst = artRes.get(ai, forceRefetch);
                		if (inst != null) {
                			// redirect to new page:
                			UI.getCurrent().getPage().open("instance/show?id="+inst.id(), "_blank");
                		//	UI.getCurrent().navigate("instance/show", new QueryParameters(Map.of("id", List.of(inst.id().toString()))));
                		}
                	}
                } catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                    log.error("Artifact Fetching Exception: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Fetching failed! \r\n"+e.getMessage());
                }
        });
        importArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);
        layout.add(p1, artTypeBox, idTypeBox, artIdField, importArtifactButton);
        return layout;
    
    }
    
    private Component dumpDesignSpace(RequestDelegate reqDel) {
    	VerticalLayout layout = new VerticalLayout();
        Paragraph p1 = new Paragraph("Dump DesignSpace:");
      
        Button dumpButton = new Button("Dump", evt -> {
        	reqDel.dumpDesignSpace();
    	});   	  
        dumpButton.addClickShortcut(Key.ENTER).listenOn(layout);
    	layout.add(p1,  dumpButton);
    	return layout;
    }

    private VerticalLayout statePanel() {
        grid = new WorkflowTreeGrid(commandGateway);
        grid.initTreeGrid();
        grids.add(grid);
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.setWidthFull();
        layout.setFlexGrow(0);

        layout.add(
                grid,
                currentStateControls(grid)
        );
        return layout;
    }

}
