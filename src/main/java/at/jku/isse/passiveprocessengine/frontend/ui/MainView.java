package at.jku.isse.passiveprocessengine.frontend.ui;

import at.jku.isse.designspace.core.controlflow.ControlEventEngine;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;

import com.vaadin.componentfactory.ToggleButton;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.UIScope;

import artifactapi.ArtifactIdentifier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
@Route("home")
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Dashboard")
public class MainView extends VerticalLayout implements HasUrlParameter<String> /*implements PageConfigurator*/ {

    private boolean devMode = false;
    public static final boolean anonymMode = false;
    
    private RequestDelegate commandGateway;
    
    private IFrontendPusher pusher;
    private ControlEventEngine cee;

    private @Getter List<WorkflowTreeGrid> grids = new ArrayList<>();

    @Inject
    public void setCommandGateway(RequestDelegate commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Inject
    public void setPusher(IFrontendPusher pusher) {
        this.pusher = pusher;
    }

    @Inject
    public void setControlEventEngine(ControlEventEngine cee) {
    	this.cee = cee;
    }
    
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
        String key = parametersMap.getOrDefault("key", List.of("")).get(0);
        String value = parametersMap.getOrDefault("value", List.of("")).get(0);
        String name = parametersMap.getOrDefault("name", List.of("")).get(0);
        initAccordion(key, value, name);
    }

//    @Override
//    public void configurePage(InitialPageSettings settings) {
//        HashMap<String, String> attributes = new HashMap<>();
//        attributes.put("rel", "shortcut icon");
//        settings.addLink("icons/favicon.ico", attributes);
//    }

    public MainView() {
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
        firstPart.add(new Icon(VaadinIcon.CLUSTER), new Label(""), new Text("Process Dashboard"));

        ToggleButton toggle = new ToggleButton("Refresher ");
        toggle.setClassName("med");
        toggle.addValueChangeListener(evt -> {
            devMode = !devMode;
//            if (devMode) {
//                Notification.show("Development mode enabled! Additional features activated.");
//            }
            initAccordion();
            content();
        });

        /*
        Icon shutdown = new Icon(VaadinIcon.POWER_OFF);
        shutdown.setColor("red");
        shutdown.getStyle().set("cursor", "pointer");
        shutdown.addClickListener(e -> SpringApp.shutdown());
        shutdown.getElement().setProperty("title", "Shut down Process Dashboard");
        */

        header.add(firstPart, toggle/*, shutdown*/);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setClassName("footer-theme");
        if (anonymMode)        
        	footer.add(new Text("(C) 2022 - Anonymized "));
        else 
        	footer.add(new Text("JKU - Institute for Software Systems Engineering"));
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
       // Tab tab1 = new Tab("Current State");
        VerticalLayout cur = statePanel(false);
        cur.setHeight("100%");


//        Tab tab2 = new Tab("Snapshot State");
//        tab2.setEnabled(devMode);
//        VerticalLayout snap = snapshotPanel(false);
//        snap.setHeight("100%");
//        snap.setVisible(false);

//        Tab tab3 = new Tab("Compare");
//        tab3.setEnabled(devMode);
//        VerticalLayout split = new VerticalLayout();
//        split.setClassName("layout-style");
//        split.add(statePanel(true), snapshotPanel(true));
//        split.setVisible(false);

 //       Map<Tab, Component> tabsToPages = new HashMap<>();
  //      tabsToPages.put(tab1, cur);
//        tabsToPages.put(tab2, snap);
//        tabsToPages.put(tab3, split);
//        Tabs tabs = new Tabs(tab1); //, tab2, tab3
        Div pages = new Div(cur); //, snap, split
        pages.setHeight("97%");
        pages.setWidthFull();

//        tabs.addSelectedChangeListener(event -> {
//            tabsToPages.values().forEach(page -> page.setVisible(false));
//            Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
//            selectedPage.setVisible(true);
//        });

        pageContent.removeAll();
        pageContent.setClassName("layout-style");
        pageContent.add(/*tabs,*/ pages);
        return pageContent;
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

    private void initAccordion(String key, String val, String name) {
        accordion.getChildren().forEach(c -> accordion.remove(c));
        accordion.add("Create Process Instance", importArtifact());
    //    accordion.add("Fetch Updates", updates());
        if (commandGateway != null && !anonymMode) {
        	accordion.add("Fetch Artifact", fetchArtifact(commandGateway.getArtifactResolver()));
            accordion.add("Dump Designspace", dumpDesignSpace(commandGateway));
        }

        accordion.add("Filter", filterTable(key, val, name));
    //    if (devMode) accordion.add("Backend Queries", backend());
        accordion.close();
        accordion.open(0);
        accordion.setWidthFull();
    }

    private Component filterTable(String k, String v, String n) {
        Paragraph p1 = new Paragraph("Filter on process properties:");
        TextField key = new TextField();
        key.setLabel("KEY");
        key.setValue(k);
        TextField val = new TextField();
        val.setLabel("VALUE");
        val.setValue(v);
        val.setValueChangeMode(ValueChangeMode.EAGER);
        key.setValueChangeMode(ValueChangeMode.EAGER);
        HorizontalLayout line = new HorizontalLayout();
        line.setWidthFull();
        line.add(key, val);
        Paragraph p2 = new Paragraph("Filter on process name:");
        TextField name = new TextField();
        name.setLabel("NAME");
        name.setValue(n);
        name.setValueChangeMode(ValueChangeMode.EAGER);

        val.addValueChangeListener(e -> {
            Map<String, String> filter = new HashMap<>();
            filter.put(key.getValue(), val.getValue());
            grids.forEach(grid -> grid.setFilters(filter, name.getValue()));
        });
        key.addValueChangeListener(e -> {
            Map<String, String> filter = new HashMap<>();
            filter.put(key.getValue(), val.getValue());
            grids.forEach(grid -> grid.setFilters(filter, name.getValue()));
        });
        name.addValueChangeListener(e -> {
            Map<String, String> filter = new HashMap<>();
            filter.put(key.getValue(), val.getValue());
            grids.forEach(grid -> grid.setFilters(filter, name.getValue()));
        });

        Map<String, String> filter = new HashMap<>();
        filter.put(key.getValue(), val.getValue());
        grids.forEach(grid -> grid.setFilters(filter, name.getValue()));

        Button clearFilters = new Button("Clear all Filters", e -> {
            val.setValue("");
            key.setValue("");
            name.setValue("");
        });
        clearFilters.addThemeVariants(ButtonVariant.LUMO_ERROR);

        return new VerticalLayout(p2, name, p1, line, clearFilters);
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
    			pusher.updateAll();
    	} else {
    		Notification.show("Toogle Refresher to trigger backend update!");
    		pusher.updateAll();
    	}
    }
    

//    private Component snapshotStateControls(WorkflowTreeGrid grid, ProgressBar progressBar) {
//        VerticalLayout layoutV = new VerticalLayout();
//        layoutV.setWidthFull();
//        layoutV.setMargin(false);
//        layoutV.setPadding(false);
//        HorizontalLayout layout2 = new HorizontalLayout();
//        layout2.setWidthFull();
//        layout2.setMargin(false);
//        layout2.setPadding(false);
//        layout2.setAlignItems(Alignment.BASELINE);
//        HorizontalLayout layout = new HorizontalLayout();
//        layout.setWidthFull();
//        layout.setMargin(false);
//        layout.setPadding(false);
//        layout.setAlignItems(Alignment.BASELINE);
//        // Date Picker
//        DatePicker valueDatePicker = new DatePicker();
//        LocalDate now = LocalDate.now();
//        valueDatePicker.setValue(now);
//        valueDatePicker.setLabel("Date");
//        // Time Picker
//        Instant time = Instant.now();
//        NumberField hour = new NumberField();
//        hour.setValue((double) time.atZone(ZoneId.systemDefault()).getHour());
//        hour.setHasControls(true);
//        hour.setMin(0);
//        hour.setMax(24);
//        hour.setLabel("Hour");
//        NumberField min = new NumberField();
//        min.setValue((double) time.atZone(ZoneId.systemDefault()).getMinute());
//        min.setHasControls(true);
//        min.setMin(0);
//        min.setMax(59);
//        min.setLabel("Minute");
//        NumberField sec = new NumberField();
//        sec.setValue((double) time.atZone(ZoneId.systemDefault()).getSecond());
//        sec.setHasControls(true);
//        sec.setMin(0);
//        sec.setMax(59);
//        sec.setLabel("Second");
//        layout.add(hour, min, sec);
//
//        // Buttons
//        Button step = new Button("Apply next Event");
//        Button jump = new Button("Apply Events until");
//        Button stop = new Button("Stop current Replay");
//
//        step.addClickListener(e -> {
//            if (snapshotter.step()) {
//                grid.updateTreeGrid(snapshotter.getState());
//                progressBar.setValue(snapshotter.getProgress());
//            } else {
//                step.setEnabled(false);
//                jump.setEnabled(false);
//                Notification.show("Last event reached!");
//            }
//        });
//        step.setEnabled(false);
//
//        jump.addClickListener(e -> {
//            LocalDateTime jumpTime = LocalDateTime.of(valueDatePicker.getValue().getYear(),
//                    valueDatePicker.getValue().getMonth().getValue(),
//                    valueDatePicker.getValue().getDayOfMonth(),
//                    hour.getValue().intValue(),
//                    min.getValue().intValue(),
//                    sec.getValue().intValue());
//            if (snapshotter.jump(jumpTime.atZone(ZoneId.systemDefault()).toInstant())) {
//                grid.updateTreeGrid(snapshotter.getState());
//                progressBar.setValue(snapshotter.getProgress());
//            } else {
//                Notification.show("Specified time is after the last or before the first event!");
//            }
//        });
//        jump.setEnabled(false);
//
//        stop.addClickListener(e -> {
//            snapshotter.quit();
//            progressBar.setValue(0);
//            grid.updateTreeGrid(Collections.emptyList());
//            step.setEnabled(false);
//            jump.setEnabled(false);
//            stop.setEnabled(false);
//        });
//        stop.setEnabled(false);
//        stop.addThemeVariants(ButtonVariant.LUMO_ERROR);
//
//        // Snapshot Button
//        Button snapshotButton = new Button("Start new Replay", evt -> {
//            LocalDateTime snapshotTime = LocalDateTime.of(valueDatePicker.getValue().getYear(),
//                    valueDatePicker.getValue().getMonth().getValue(),
//                    valueDatePicker.getValue().getDayOfMonth(),
//                    hour.getValue().intValue(),
//                    min.getValue().intValue(),
//                    sec.getValue().intValue());
//            if (snapshotter.start(snapshotTime.atZone(ZoneId.systemDefault()).toInstant())) {
//                grid.updateTreeGrid(snapshotter.getState());
//                progressBar.setValue(snapshotter.getProgress());
//                step.setEnabled(true);
//                jump.setEnabled(true);
//                stop.setEnabled(true);
//            } else {
//                Notification.show("Specified time is after the last or before the first event!");
//            }
//        });
//
//        layout.add(valueDatePicker);
//        layout2.add(snapshotButton, step, jump, stop);
//        layoutV.add(layout, layout2);
//        return layoutV;
//    }


    private Component importArtifact() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setWidth("90%");
        Set<String> wfdKeys = new HashSet<>();
        // Process Definition
        if (commandGateway != null)
        	wfdKeys.addAll(commandGateway.getRegistry().getAllDefinitionIDs());

        RadioButtonGroup<String> processDefinition = new RadioButtonGroup<>();
        processDefinition.setItems(wfdKeys);
        processDefinition.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);


        Button loadDefinitions = new Button("Fetch Available Definitions", e -> {
            initAccordion();
        });

//        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
//        Upload upload = new Upload(buffer);
//        upload.setWidthFull();
//        Div output = new Div();
//        upload.addSucceededListener(event -> {
//            Component component = createComponent(event.getMIMEType(),
//                    event.getFileName(),
//                    buffer.getInputStream(event.getFileName()));
//            showOutput(event.getFileName(), component, output);
//        });

//        Button addDefinition = new Button("Store New Definition", e -> {
//            try {
//                Map<String, String> ruleFiles = new HashMap<>();
//                String json = null;
//                for (String filename : buffer.getFiles()) {
//                    if (filename.endsWith(".json")) {
//                        json = IOUtils.toString(buffer.getInputStream(filename), StandardCharsets.UTF_8.name());
//                    } else if (filename.endsWith(".drl")) {
//                        ruleFiles.put(filename, IOUtils.toString(buffer.getInputStream(filename), StandardCharsets.UTF_8.name()));
//                    } else {
//                        // not allowed
//                    }
//                }
//                if (json != null && ruleFiles.size() > 0) {
//                    registry.register(json, ruleFiles);
//                    processDefinition.setItems(registry == null ? Collections.emptySet() : registry.getAll().keySet());
//                    Notification.show("Workflow loaded and added to registry");
//                } else {
//                    Notification.show("Make sure to have exactly one JSON file and at least one DRL file in the upload");
//                }
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        });

        // Source
        VerticalLayout source = new VerticalLayout();
        source.setMargin(false);
        source.setPadding(false);
        source.setWidthFull();
        source.add(new Paragraph("select a process definition"));
        processDefinition.addValueChangeListener( e -> {
            ProcessDefinition wfdContainer = commandGateway.getRegistry().getProcessDefinition(e.getValue()).get(); // we fetched the ids earlier, should exist here
            source.removeAll();
            for (Map.Entry<String, InstanceType> entry : wfdContainer.getExpectedInput().entrySet()) {
                InstanceType artT = entry.getValue();
                String role = entry.getKey();
                TextField tf = new TextField();
                tf.setWidthFull();
                tf.setLabel(role);
                if (artT != null)
                	tf.setHelperText(artT.name());
                else 
                	tf.setHelperText("Unknown Type");
                source.add(tf);
            }
            if (wfdContainer.getExpectedInput().size() == 0) {
                source.add(new Paragraph("no inputs expected"));
            }
        });

        Button importOrUpdateArtifactButton = new Button("Create", evt -> {
            if (processDefinition.getValue() == null) {
                Notification.show("Select a Process Definition first!");
            } else {
                try {
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
                                ArtifactIdentifier ai = new ArtifactIdentifier(tf.getValue().trim(), tf.getHelperText().trim());
                                inputs.put(tf.getLabel().trim(),ai);
                            });
                    // send command
                    if (count.get() == inputs.size()) {
                    	//inputs.keySet().stream().map(ai -> ai.get)
                        String id = inputs.values().stream().map(ai -> ai.getId()).collect(Collectors.joining(""))+processDefinition.getValue(); //getNewId()
                    	commandGateway.instantiateProcess(id, inputs, processDefinition.getValue());
                        Notification.show("Success");
                    } else {
                        Notification.show("Make sure to fill out all required artifact IDs!");
                    }
                } catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                    log.error("CommandExecutionException: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Creation failed! \r\n"+e.getMessage());
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
    	ComboBox<String> comboBox = new ComboBox<>("Instance Type");
    	List<String> instTypes = List.of("git_issue", "azure_workitem", "jira_core_artifact");
    	comboBox.setItems(instTypes);
    	//comboBox.setMinWidth("400px");
    	
    	Button importArtifactButton = new Button("Fetch", evt -> {
            
                try {
                	if (comboBox.getOptionalValue().isEmpty())
            			Notification.show("Make sure to select an Artifact Type!");
                	else if (artIdField.getValue().length() < 1)
                		Notification.show("Make sure to provide an identifier!");
                	else {
                		ArtifactIdentifier ai = new ArtifactIdentifier(artIdField.getValue().trim(), comboBox.getOptionalValue().get());
                		Instance inst = artRes.get(ai);
                		if (inst != null) {
                			// redirect to new page:
                			UI.getCurrent().navigate("instance/show", new QueryParameters(Map.of("id", List.of(inst.id().toString()))));
                		}
                	}
                } catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                    log.error("Artifact Fetching Exception: " + e.getMessage());
                    e.printStackTrace();
                    Notification.show("Fetching failed! \r\n"+e.getMessage());
                }
        });
        importArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);
        layout.add(p1, comboBox, artIdField, importArtifactButton);
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

    private VerticalLayout statePanel(boolean addHeader) {
        WorkflowTreeGrid grid = new WorkflowTreeGrid(commandGateway);
        grid.initTreeGrid();
        grids.add(grid);
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.setWidthFull();
        layout.setFlexGrow(0);
//        if (addHeader)
//            layout.add(new Text("Current State"));
        layout.add(
                grid,
                currentStateControls(grid)
        );
        return layout;
    }

}
