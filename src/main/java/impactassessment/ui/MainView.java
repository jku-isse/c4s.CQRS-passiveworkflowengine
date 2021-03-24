package impactassessment.ui;

import artifactapi.ArtifactType;
import artifactapi.jira.IJiraArtifact;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jiralightconnector.MonitoringScheduler;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.vaadin.componentfactory.ToggleButton;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;
import impactassessment.SpringApp;
import impactassessment.SpringUtil;
import impactassessment.api.Commands.CheckConstraintCmd;
import impactassessment.api.Commands.CreateMockWorkflowCmd;
import impactassessment.api.Commands.CreateWorkflowCmd;
import impactassessment.api.Commands.DeleteCmd;
import impactassessment.api.Queries.GetStateQuery;
import impactassessment.api.Queries.GetStateResponse;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.api.Queries.PrintKBResponse;
import impactassessment.artifactconnector.jira.mock.JiraMockService;
import impactassessment.evaluation.JamaUpdatePerformanceService;
import impactassessment.evaluation.JamaWorkflowCreationPerformanceService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.query.Replayer;
import impactassessment.query.Snapshotter;
import impactassessment.registry.WorkflowDefinitionContainer;
import impactassessment.registry.WorkflowDefinitionRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static impactassessment.general.IdGenerator.getNewId;
import static impactassessment.ui.Helpers.createComponent;
import static impactassessment.ui.Helpers.showOutput;

@Slf4j
@Route
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Dashboard")
public class MainView extends VerticalLayout /*implements PageConfigurator*/ {

    private boolean devMode = false;

    private CommandGateway commandGateway;
    private QueryGateway queryGateway;
    private Snapshotter snapshotter;
    private Replayer replayer;
    private WorkflowDefinitionRegistry registry;
    private IFrontendPusher pusher;
    private MonitoringScheduler jiraMonitoringScheduler;
    private c4s.jamaconnector.MonitoringScheduler jamaMonitoringScheduler;

    private @Getter List<WorkflowTreeGrid> grids = new ArrayList<>();

    @Inject
    public void setCommandGateway(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }
    @Inject
    public void setQueryGateway(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }
    @Inject
    public void setSnapshotter(Snapshotter snapshotter) {
        this.snapshotter = snapshotter;
    }
    @Inject
    public void setReplayer(Replayer replayer) {
        this.replayer = replayer;
    }
    @Inject
    public void setProcessDefinitionRegistry(WorkflowDefinitionRegistry registry) {
        this.registry = registry;
    }
    @Inject
    public void setPusher(IFrontendPusher pusher) {
        this.pusher = pusher;
    }
    @Inject
    public void setJiraMonitoringScheduler(MonitoringScheduler jiraMonitoringScheduler) {
        this.jiraMonitoringScheduler = jiraMonitoringScheduler;
    }
    @Inject
    public void setJamaMonitoringScheduler(c4s.jamaconnector.MonitoringScheduler jamaMonitoringScheduler) {
        this.jamaMonitoringScheduler = jamaMonitoringScheduler;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        pusher.setUi(attachEvent.getUI());
        pusher.setView(this);
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

        ToggleButton toggle = new ToggleButton("Dev Mode ");
        toggle.setClassName("med");
        toggle.addValueChangeListener(evt -> {
            devMode = !devMode;
            if (devMode) {
                Notification.show("Development mode enabled! Additional features activated.");
            }
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
        Tab tab1 = new Tab("Current State");
        VerticalLayout cur = statePanel(false);
        cur.setHeight("100%");


        Tab tab2 = new Tab("Snapshot State");
        tab2.setEnabled(devMode);
        VerticalLayout snap = snapshotPanel(false);
        snap.setHeight("100%");
        snap.setVisible(false);

        Tab tab3 = new Tab("Compare");
        tab3.setEnabled(devMode);
        VerticalLayout split = new VerticalLayout();
        split.setClassName("layout-style");
        split.add(statePanel(true), snapshotPanel(true));
        split.setVisible(false);

        Map<Tab, Component> tabsToPages = new HashMap<>();
        tabsToPages.put(tab1, cur);
        tabsToPages.put(tab2, snap);
        tabsToPages.put(tab3, split);
        Tabs tabs = new Tabs(tab1, tab2, tab3);
        Div pages = new Div(cur, snap, split);
        pages.setHeight("97%");
        pages.setWidthFull();

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });

        pageContent.removeAll();
        pageContent.setClassName("layout-style");
        pageContent.add(tabs, pages);
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
        accordion.getChildren().forEach(c -> accordion.remove(c));
        accordion.add("Create Process Instance", importArtifact(devMode));
        if (devMode) accordion.add("Create Mock-Process Instance", importMocked());
        accordion.add("Fetch Updates", updates());
        accordion.add("Filter", filterTable());
//        accordion.add("Remove Workflow", remove()); // functionality provided via icon in the table
//        accordion.add("Evaluate Constraint", evaluate()); // functionality provided via icon in the table
        if (devMode) accordion.add("Backend Queries", backend());
        accordion.close();
        accordion.open(0);
        accordion.setWidthFull();
    }

    private Component filterTable() {
        Paragraph p = new Paragraph("Filter on process properties:");
        TextField key = new TextField();
        key.setPlaceholder("KEY");
        TextField val = new TextField();
        val.setPlaceholder("VALUE");
        val.setValueChangeMode(ValueChangeMode.EAGER);
        val.addValueChangeListener(e -> {
            Map<String, String> filter = new HashMap<>();
            if (!key.getValue().equals("") || !val.getValue().equals(""))
                filter.put(key.getValue(), val.getValue());
            grids.forEach(grid -> grid.updateTreeGrid(filter));
        });
        key.setValueChangeMode(ValueChangeMode.EAGER);
        key.addValueChangeListener(e -> {
            Map<String, String> filter = new HashMap<>();
            if (!key.getValue().equals("") || !val.getValue().equals(""))
                filter.put(key.getValue(), val.getValue());
            grids.forEach(grid -> grid.updateTreeGrid(filter));
        });
        HorizontalLayout line = new HorizontalLayout();
        line.setWidthFull();
        line.add(key, val);
        return new VerticalLayout(p, line);
    }

    private Component currentStateControls(WorkflowTreeGrid grid) {
        HorizontalLayout controlButtonLayout = new HorizontalLayout();
        controlButtonLayout.setMargin(false);
        controlButtonLayout.setPadding(false);
        controlButtonLayout.setWidthFull();

        Button getState = new Button("Refresh");
        getState.addClickListener(evt -> {
            CompletableFuture<GetStateResponse> future = queryGateway.query(new GetStateQuery(0), GetStateResponse.class);
            try {
                Notification.show("Current State refreshing requested");
                List<WorkflowInstanceWrapper> response = future.get(5, TimeUnit.SECONDS).getState();
                grid.updateTreeGrid(response);
                Notification.show("Refresh successful!");
            } catch (TimeoutException e1) {
                log.error("GetStateQuery resulted in TimeoutException, make sure projection is initialized (Replay all Events first)!");
                Notification.show("TimeoutException: Either projection is out of sync ('Replay All Events' might help) or a server exception occurred.");
            } catch (InterruptedException | ExecutionException e2) {
                log.error("GetStateQuery resulted in Exception: "+e2.getMessage());
            }
        });
        Button replay = new Button("Replay All Events", evt -> {
            Notification.show("Replay of Current State initiated. Replay gets executed..");
            replayer.replay("projection");
        });

        controlButtonLayout.add(getState, replay);
        return controlButtonLayout;
    }

    private Component snapshotStateControls(WorkflowTreeGrid grid, ProgressBar progressBar) {
        VerticalLayout layoutV = new VerticalLayout();
        layoutV.setWidthFull();
        layoutV.setMargin(false);
        layoutV.setPadding(false);
        HorizontalLayout layout2 = new HorizontalLayout();
        layout2.setWidthFull();
        layout2.setMargin(false);
        layout2.setPadding(false);
        layout2.setAlignItems(Alignment.BASELINE);
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setMargin(false);
        layout.setPadding(false);
        layout.setAlignItems(Alignment.BASELINE);
        // Date Picker
        DatePicker valueDatePicker = new DatePicker();
        LocalDate now = LocalDate.now();
        valueDatePicker.setValue(now);
        valueDatePicker.setLabel("Date");
        // Time Picker
        Instant time = Instant.now();
        NumberField hour = new NumberField();
        hour.setValue((double) time.atZone(ZoneId.systemDefault()).getHour());
        hour.setHasControls(true);
        hour.setMin(0);
        hour.setMax(24);
        hour.setLabel("Hour");
        NumberField min = new NumberField();
        min.setValue((double) time.atZone(ZoneId.systemDefault()).getMinute());
        min.setHasControls(true);
        min.setMin(0);
        min.setMax(59);
        min.setLabel("Minute");
        NumberField sec = new NumberField();
        sec.setValue((double) time.atZone(ZoneId.systemDefault()).getSecond());
        sec.setHasControls(true);
        sec.setMin(0);
        sec.setMax(59);
        sec.setLabel("Second");
        layout.add(hour, min, sec);

        // Buttons
        Button step = new Button("Apply next Event");
        Button jump = new Button("Apply Events until");
        Button stop = new Button("Stop current Replay");

        step.addClickListener(e -> {
            if (snapshotter.step()) {
                grid.updateTreeGrid(snapshotter.getState());
                progressBar.setValue(snapshotter.getProgress());
            } else {
                step.setEnabled(false);
                jump.setEnabled(false);
                Notification.show("Last event reached!");
            }
        });
        step.setEnabled(false);

        jump.addClickListener(e -> {
            LocalDateTime jumpTime = LocalDateTime.of(valueDatePicker.getValue().getYear(),
                    valueDatePicker.getValue().getMonth().getValue(),
                    valueDatePicker.getValue().getDayOfMonth(),
                    hour.getValue().intValue(),
                    min.getValue().intValue(),
                    sec.getValue().intValue());
            if (snapshotter.jump(jumpTime.atZone(ZoneId.systemDefault()).toInstant())) {
                grid.updateTreeGrid(snapshotter.getState());
                progressBar.setValue(snapshotter.getProgress());
            } else {
                Notification.show("Specified time is after the last or before the first event!");
            }
        });
        jump.setEnabled(false);

        stop.addClickListener(e -> {
            snapshotter.quit();
            progressBar.setValue(0);
            grid.updateTreeGrid(Collections.emptyList());
            step.setEnabled(false);
            jump.setEnabled(false);
            stop.setEnabled(false);
        });
        stop.setEnabled(false);
        stop.addThemeVariants(ButtonVariant.LUMO_ERROR);

        // Snapshot Button
        Button snapshotButton = new Button("Start new Replay", evt -> {
            LocalDateTime snapshotTime = LocalDateTime.of(valueDatePicker.getValue().getYear(),
                    valueDatePicker.getValue().getMonth().getValue(),
                    valueDatePicker.getValue().getDayOfMonth(),
                    hour.getValue().intValue(),
                    min.getValue().intValue(),
                    sec.getValue().intValue());
            if (snapshotter.start(snapshotTime.atZone(ZoneId.systemDefault()).toInstant())) {
                grid.updateTreeGrid(snapshotter.getState());
                progressBar.setValue(snapshotter.getProgress());
                step.setEnabled(true);
                jump.setEnabled(true);
                stop.setEnabled(true);
            } else {
                Notification.show("Specified time is after the last or before the first event!");
            }
        });

        layout.add(valueDatePicker);
        layout2.add(snapshotButton, step, jump, stop);
        layoutV.add(layout, layout2);
        return layoutV;
    }


    private Component importArtifact(boolean devMode) {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setWidth("90%");

        // Process Definition
        Set<String> wfdKeys;
        if (registry != null) {
            wfdKeys = registry.getAll().keySet();
        } else {
            wfdKeys = Collections.emptySet();
        }
        RadioButtonGroup<String> processDefinition = new RadioButtonGroup<>();
        processDefinition.setItems(wfdKeys);
        processDefinition.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);


        Button loadDefinitions = new Button("Fetch Available Definitions", e -> {
            initAccordion();
        });

        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setWidthFull();
        Div output = new Div();
        upload.addSucceededListener(event -> {
            Component component = createComponent(event.getMIMEType(),
                    event.getFileName(),
                    buffer.getInputStream(event.getFileName()));
            showOutput(event.getFileName(), component, output);
        });

        Button addDefinition = new Button("Store New Definition", e -> {
            try {
                Map<String, String> ruleFiles = new HashMap<>();
                String json = null;
                for (String filename : buffer.getFiles()) {
                    if (filename.endsWith(".json")) {
                        json = IOUtils.toString(buffer.getInputStream(filename), StandardCharsets.UTF_8.name());
                    } else if (filename.endsWith(".drl")) {
                        ruleFiles.put(filename, IOUtils.toString(buffer.getInputStream(filename), StandardCharsets.UTF_8.name()));
                    } else {
                        // not allowed
                    }
                }
                if (json != null && ruleFiles.size() > 0) {
                    registry.register(json, ruleFiles);
                    processDefinition.setItems(registry == null ? Collections.emptySet() : registry.getAll().keySet());
                    Notification.show("Workflow loaded and added to registry");
                } else {
                    Notification.show("Make sure to have exactly one JSON file and at least one DRL file in the upload");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Source
        VerticalLayout source = new VerticalLayout();
        source.setMargin(false);
        source.setPadding(false);
        source.setWidthFull();
        source.add(new Paragraph("select a process definition"));
        processDefinition.addValueChangeListener( e -> {
            WorkflowDefinitionContainer wfdContainer = registry.get(e.getValue());
            source.removeAll();
            for (Map.Entry<String, ArtifactType> entry : wfdContainer.getWfd().getExpectedInput().entrySet()) {
                ArtifactType artT = entry.getValue();
                String role = entry.getKey();
                TextField tf = new TextField();
                tf.setWidthFull();
                //xxx // no hardcoded types here, implicit dependency to command handler very ugly!
//                if (artT.getArtifactType().equals(IJiraArtifact.class.getSimpleName())) {
//                    tf.setLabel(role+": JIRA");
//                } else if (artT.getArtifactType().equals(IJamaArtifact.class.getSimpleName())) {
//                    tf.setLabel(role+": JAMA");
//                } else {
//                    tf.setLabel(artT.getArtifactType());
//                }
                //FIXME: workaround to display type and role
                tf.setLabel(role+"::"+artT.getArtifactType());
                source.add(tf);
            }
            if (wfdContainer.getWfd().getExpectedInput().size() == 0) {
                source.add(new Paragraph("no inputs expected"));
            }
        });

        Button importOrUpdateArtifactButton = new Button("Create", evt -> {
            if (processDefinition.getValue() == null) {
                Notification.show("Select a Process Definition first!");
            } else {
                try {
                    // collect all input IDs
                    Map<String, String> inputs = new HashMap<>();
                    AtomicInteger count = new AtomicInteger();
                    source.getChildren()
                            .filter(child -> child instanceof TextField)
                            .map(child -> {
                                count.getAndIncrement();
                                return (TextField) child;
                            })
                            .filter(tf -> !tf.getValue().equals(""))
                            .filter(tf -> !tf.getLabel().equals(""))
                            // to be consistent with changes above
                            .forEach(tf -> inputs.put(tf.getValue().trim(), tf.getLabel().trim()));
                    //.forEach(tf -> inputs.put(tf.getValue(), tf.getLabel().substring(tf.getLabel().lastIndexOf(": ")+2)));
                    // send command
                    if (count.get() == inputs.size()) {
                        commandGateway.sendAndWait(new CreateWorkflowCmd(getNewId(), inputs, processDefinition.getValue()));
                        Notification.show("Success");
                    } else {
                        Notification.show("Make sure to fill out all required artifact IDs!");
                    }
                } catch (CommandExecutionException e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                    log.error("CommandExecutionException: " + e.getMessage());
                    Notification.show("Creation failed!");
                }
            }
        });
        importOrUpdateArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);

        if (wfdKeys.isEmpty()) {
            Paragraph par = new Paragraph("fetch available definitions/add new ones (adding requires 'dev mode' switched on)");
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
        if (devMode) layout.add(
                upload,
                addDefinition);
        layout.add(
                new H4("2. Enter Artifact ID(s)"),
                source,
                importOrUpdateArtifactButton);
        return layout;
    }

    private void enable(boolean enable, TextField... fields) {
        for (TextField field : fields) {
            field.setEnabled(enable);
        }
    }

    private Component evaluate() {
        TextField id = new TextField("Artifact ID");
        id.setValue("A3");

        TextField corr = new TextField("Constraint ID");
        corr.setValue("CheckAllRelatedBugsClosed_Resolved_A3");
        corr.setWidthFull();

        Button check = new Button("Check");
        check.addClickListener(evt -> {
            commandGateway.sendAndWait(new CheckConstraintCmd(id.getValue(), corr.getValue()));
            Notification.show("Success");
        });

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setPadding(false);
        layout.setWidthFull();
        layout.add(id, corr, check);

        return layout;
    }

    private Component backend() {
        Text description = new Text("Commands that get processed by the backend. Effects can only be observed on the server log.");
        TextField id = new TextField("ID");
        id.setValue("A3");

        Button print = new Button("Log KB-Content to Console");
        print.addClickListener(evt -> {
            queryGateway.query(new PrintKBQuery(id.getValue()), PrintKBResponse.class);
            Notification.show("Success");
        });

        //---------------------------- Jira Poller -----------------------------
//        timer.setHeight("20px");
//        timer.addClassName("big-text");
//        timer.setVisible(false);
//        TextField textField = new TextField();
//        textField.setLabel("Update Interval in Minutes");
//        textField.setValue("1");
//
//        Checkbox checkbox = new Checkbox("Enable Automatic updates");
//        checkbox.setValue(false);
//        checkbox.addValueChangeListener(e -> {
//            if (e.getValue()) {
//                try {
//                    int interval = Integer.parseInt(textField.getValue());
//                    textField.setEnabled(false);
//                    timer.setStartTime(new BigDecimal(interval*60));
//                    timer.setVisible(true);
//                    timer.start();
//                    jiraPoller.setInterval(interval);
//                    jiraPoller.start();
//                } catch (NumberFormatException ex) {
//                    Notification.show("Please enter a number");
//                }
//            } else {
//                jiraPoller.interrupt();
//                textField.setEnabled(true);
//                timer.setVisible(false);
//                timer.pause();
//                timer.reset();
//            }
//        });
        //---------------------------------------------------------

        Button jamaPerformancetest1 = new Button("Process Creation Performance Test", e -> {
            JamaWorkflowCreationPerformanceService service1 = SpringUtil.getBean(JamaWorkflowCreationPerformanceService.class);
            service1.createAll();
        });
        Button jamaPerformancetest2 = new Button("Update Artifacts Performance Test", e -> {
            JamaUpdatePerformanceService service2 = SpringUtil.getBean(JamaUpdatePerformanceService.class);
            service2.replayUpdates();
        });
        return new VerticalLayout(description, id, print, /*timer, textField, checkbox,*/ jamaPerformancetest1, jamaPerformancetest2);
    }

    private Component remove() {
        VerticalLayout layout = new VerticalLayout();
        TextField id = new TextField("ID");
        id.setValue("A3");

        Button removeArtifactButton = new Button("Remove Artifact");
        removeArtifactButton.addClickListener(evt -> {
            commandGateway.send(new DeleteCmd(id.getValue()));
        });
        removeArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);

        layout.add(id, removeArtifactButton);
        return layout;
    }

    private Component importMocked() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setPadding(false);
        layout.setWidthFull();

        // MOCK fields
        TextField id = new TextField("ID");
        id.setValue("JiraMock1");
        id.setWidthFull();
        TextField status = new TextField("Status");
        status.setValue(JiraMockService.DEFAULT_STATUS);
        status.setWidthFull();
        TextField issuetype = new TextField("Issue-Type");
        issuetype.setValue(JiraMockService.DEFAULT_ISSUETYPE);
        issuetype.setWidthFull();
        TextField priority = new TextField("Priority");
        priority.setValue(JiraMockService.DEFAULT_PRIORITY);
        priority.setWidthFull();
        TextField summary = new TextField("Summary");
        summary.setValue(JiraMockService.DEFAULT_SUMMARY);
        summary.setWidthFull();

        Button importOrUpdateArtifactButton = new Button("Create", evt -> {
            try {
                commandGateway.sendAndWait(new CreateMockWorkflowCmd(id.getValue(), status.getValue(), issuetype.getValue(), priority.getValue(), summary.getValue()));
                Notification.show("Success");
            } catch (CommandExecutionException e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                log.error("CommandExecutionException: "+e.getMessage());
                Notification.show("Creation failed!");
            }
        });
        importOrUpdateArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);

        VerticalLayout column1 = new VerticalLayout();
        column1.setMargin(false);
        column1.setPadding(false);
        column1.add(id, status);
        column1.setWidth("50%");
        VerticalLayout column2 = new VerticalLayout();
        column2.setMargin(false);
        column2.setPadding(false);
        column2.add(issuetype, priority);
        column2.setWidth("50%");
        HorizontalLayout row1 = new HorizontalLayout(column1, column2);
        row1.setWidthFull();
        row1.setMargin(false);
        row1.setPadding(false);
        VerticalLayout row2 = new VerticalLayout();
        row2.setMargin(false);
        row2.setPadding(false);
        row2.add(summary, importOrUpdateArtifactButton);

        layout.add(new Paragraph("No actual artifact will be fetched, but a mocked version as specified below will be used."), row1, row2);
        return layout;
    }

//    private @Getter
//    SimpleTimer timer = new SimpleTimer(60);
    private Component updates() {

        Button update = new Button("Fetch Updates Now", e -> {
                jiraMonitoringScheduler.runAllMonitoringTasksSequentiallyOnceNow(new CorrelationTuple()); // TODO which corr is needed?
                jamaMonitoringScheduler.runAllMonitoringTasksSequentiallyOnceNow(new CorrelationTuple()); // TODO which corr is needed?
        });
        String pollTime = SpringUtil.getBean(String.class, "pollIntervalInMinutes");
        return new VerticalLayout(new Paragraph("Updates are fetched every "+pollTime+" minutes automatically. Additionally you can fetch updates manually."), update);
    }

    private VerticalLayout snapshotPanel(boolean addHeader) {
        WorkflowTreeGrid grid = new WorkflowTreeGrid(x -> commandGateway.send(x), false);
        grid.initTreeGrid();
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.setWidthFull();
        layout.setFlexGrow(0);
        if (addHeader)
            layout.add(new Text("Snapshot State"));
        ProgressBar progressBar = new ProgressBar();
        progressBar.setValue(0);
        layout.add(
                progressBar,
                grid,
                snapshotStateControls(grid, progressBar)
        );
        return layout;
    }

    private VerticalLayout statePanel(boolean addHeader) {
        WorkflowTreeGrid grid = new WorkflowTreeGrid(x -> commandGateway.send(x), true);
        grid.initTreeGrid();
        grids.add(grid);
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.setWidthFull();
        layout.setFlexGrow(0);
        if (addHeader)
            layout.add(new Text("Current State"));
        layout.add(
                grid,
                currentStateControls(grid)
        );
        return layout;
    }

}
