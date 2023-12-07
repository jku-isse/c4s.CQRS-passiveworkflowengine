package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.DefinitionView.DefinitionComparator;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route(value="home", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Dashboard")
@UIScope
@RouteAlias(value = "", layout = AppView.class)
//@SpringComponent
public class MainView extends VerticalLayout implements HasUrlParameter<String>  {

    	
    @Autowired
    private RequestDelegate commandGateway;
    private ProcessConfigBaseElementFactory configFactory;
    @Autowired
    private IFrontendPusher pusher;
    private @Getter WorkflowTreeGrid grid;
    private ComboBox<ProcessDefinition> definitionsBox;
    private Details loadProcess;
    private  SplitLayout splitLayout;

   
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        pusher.add(attachEvent.getUI().getUIId(), attachEvent.getUI(), this);
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
        //initAccordion(id, name, focus);
        grid.setFilters(id, name, focus);
    }


    public MainView(RequestDelegate reqDel, IFrontendPusher pusher, ProcessConfigBaseElementFactory configFactory) {
    	 this.commandGateway = reqDel;
    	 this.pusher = pusher;
    	 this.configFactory = configFactory;
    	setSizeFull();
        setMargin(false);
        
        loadProcess = new Details("Instantiate Process", processLoader());
        loadProcess.setOpened(false);
        loadProcess.addThemeVariants(DetailsVariant.FILLED);

        Button getState = new Button("Refresh State");
        getState.addClickListener(evt -> refresh(grid));
        getState.getElement().setProperty("title", "The whole state is automatically updated on events that change the state. You can refresh the state manually with this button if necessary!");

        
        HorizontalLayout header = new HorizontalLayout(loadProcess, getState);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.START);
        header.expand(loadProcess); 
        header.setWidth("100%");
                
        ProcessInstanceScopedElementView detailsView = new ProcessInstanceScopedElementView(reqDel, configFactory);
        detailsView.setVisible(false); // initially nothing to show, hence invisible, will be made visible upon selection
        grid = new WorkflowTreeGrid(commandGateway);                
        grid.initTreeGrid();        
        grid.setHeightFull();
        grid.addSelectionListener( o -> {
    		o.getFirstSelectedItem().ifPresentOrElse(el -> { 
    			detailsView.fillDetailsView(el); 
    			}   			
    		, () -> { 
    			detailsView.fillDetailsView(null); });
    	});        
        
        splitLayout = new SplitLayout(grid, detailsView);
        splitLayout.setSplitterPosition(60);
        splitLayout.setSizeFull();        
        
        add(header, splitLayout);
        this.setAlignSelf(Alignment.STRETCH, splitLayout);
    }

    private void refresh(WorkflowTreeGrid grid) {
    		int count = commandGateway.resetAndUpdate();
    		//if (count <= 0) // otherwise there is an update called from the FrontendPusher
    		pusher.requestUpdate(getUI().get(), this); 
    }    

    private void reloadProcessDefinitions() {
    	List<ProcessDefinition> defs = commandGateway.getRegistry().getAllDefinitions(true).stream()
    			.filter(pdef -> pdef.getProcess() == null) // only top level processes shown	
				.sorted(new DefinitionComparator())
				.collect(Collectors.toList());
    	definitionsBox.setItems(defs);
    }

    private Component processLoader() {        
        //layout.setWidth("90%");
        HorizontalLayout definitions = new HorizontalLayout();
    	definitions.setAlignItems(Alignment.END);
    	definitionsBox = new ComboBox<>("Select a Process Definition");
    	reloadProcessDefinitions();
    	definitionsBox.setItemLabelGenerator(pdef -> pdef.getName() );
        definitionsBox.setMinWidth("600px");              

        Icon reloadIcon = new Icon(VaadinIcon.REFRESH);
        reloadIcon.getStyle()
	      .set("box-sizing", "border-box")
	      .set("margin-inline-end", "var(--lumo-space-m)")
	      .set("margin-inline-start", "var(--lumo-space-xs)")
	      .set("padding", "var(--lumo-space-xs)");
        Button loadDefinitions = new Button("Reload Available Definitions", reloadIcon, e -> {
            reloadProcessDefinitions();
        });
        
        definitions.add(definitionsBox, loadDefinitions);

        // Source
        VerticalLayout source = new VerticalLayout();
        source.setMargin(false);
        source.setPadding(false);
        
        Map<String, ComboBox<String>> role2IdType = new HashMap<>();
        Map<String, TextField> param2Field = new HashMap<>();
        
        definitionsBox.addValueChangeListener( e -> {
            ProcessDefinition wfdContainer = e.getValue(); 
            source.removeAll();
            role2IdType.clear();
            param2Field.clear();
            for (Map.Entry<String, InstanceType> entry : wfdContainer.getExpectedInput().entrySet()) {
            	InstanceType artT = entry.getValue();                
                String role = entry.getKey();
            	
            	HorizontalLayout inputData = new HorizontalLayout();            	
                inputData.setMargin(false);               
                inputData.setPadding(false);
                
                TextField tf = new TextField();
                tf.setRequiredIndicatorVisible(true);
                //tf.setWidthFull();
                tf.setMinWidth("600px");
                tf.setLabel(role);                
                tf.setHelperText(artT.name());
                param2Field.put(role, tf);
                inputData.add(tf);
                
                ComboBox<String> idTypeBox = new ComboBox<>("Identifier Type");
                List<String> idTypes = commandGateway.getArtifactResolver().getIdentifierTypesForInstanceType(artT);
                role2IdType.put(role, idTypeBox);
                idTypeBox.setItems(idTypes);
                idTypeBox.setValue(idTypes.get(0));
                idTypeBox.setAllowCustomValue(false);
                inputData.add(idTypeBox);
                
                if (artT.isKindOf(configFactory.getBaseType())) {
                	// this is a configuration artifact, lets make it possible to create a config on the fly, if the users wishes so
                	Icon createIcon = new Icon(VaadinIcon.PLUS);
                    createIcon.getStyle()
            	      .set("box-sizing", "border-box")
            	      .set("margin-inline-end", "var(--lumo-space-m)")
            	      .set("margin-inline-start", "var(--lumo-space-xs)")
            	      .set("padding", "var(--lumo-space-xs)");
                	Button createConfigButton = new Button("New Config", createIcon, evt -> {
                		Instance config = configFactory.createConfigInstance(role, artT);
                		tf.setValue(config.id().toString());
                	});     
                	createConfigButton.getElement().getStyle().set("margin-top","37px");
                	inputData.add(createConfigButton);
                }
                
                source.add(inputData);
            }
            if (wfdContainer.getExpectedInput().size() == 0) {
                source.add(new Paragraph("No inputs expected"));
            }
        });

        
        Icon startIcon = new Icon(VaadinIcon.FILE_START);
        startIcon.getStyle()
	      .set("box-sizing", "border-box")
	      .set("margin-inline-end", "var(--lumo-space-m)")
	      .set("margin-inline-start", "var(--lumo-space-xs)")
	      .set("padding", "var(--lumo-space-xs)");
        
        Button instantiateProcessButton = new Button("Instantiate Process", startIcon, evt -> {
        	if (definitionsBox.getValue() == null) {
        		Notification.show("Select a Process Definition first!");
        	} else {

        		// collect all input IDs
        		Map<String, ArtifactIdentifier> inputs = new LinkedHashMap<>();
        		AtomicInteger count = new AtomicInteger();
        		param2Field.values().stream()
        		.peek(child -> {
        			count.getAndIncrement();        			
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
        				String procName = definitionsBox.getValue().getName();
        				String nextAllowedProc = commandGateway.isAllowedAsNextProc(procName, auth != null ? auth.getName() : null);
        				if (!nextAllowedProc.equalsIgnoreCase(procName)) {
        					Notification.show("You are not authorized to instantiate this process now: "+nextAllowedProc);
        				} else {

        					//inputs.keySet().stream().map(ai -> ai.get)
        					String id = inputs.values().stream().map(ai -> ai.getId()).collect(Collectors.joining(""))+procName; //getNewId()
        					Notification.show("Process Instantiation might take some time. UI will be updated automatically upon success.");
        					new Thread(() -> { 
        						try {
        							ProcessInstance pi = commandGateway.instantiateProcess(id, inputs, procName);
        							if (auth != null && auth.getName() != null) {
        								pi.getInstance().addOwner(new User(auth.getName()));
        							}
        							commandGateway.getMonitor().processCreated(pi, auth != null ? auth.getName() : null);
        							this.getUI().get().access(() -> { 
        								Notification.show("Success");
        								splitLayout.setSplitterPosition(70);
        								loadProcess.setOpened(false);
        							});
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
                        
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.add(definitions);
        layout.add(source);
        layout.add(instantiateProcessButton);
        
        instantiateProcessButton.addClickShortcut(Key.ENTER).listenOn(layout);

        return layout;
    }
   
}
