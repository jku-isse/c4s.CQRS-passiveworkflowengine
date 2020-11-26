package impactassessment.ui;

import c4s.jiralightconnector.ChangeStreamPoller;
import com.flowingcode.vaadin.addons.simpletimer.SimpleTimer;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.router.Route;
import impactassessment.SpringUtil;
import impactassessment.jiraartifact.JiraPoller;
import impactassessment.jiraartifact.mock.JiraMockService;
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
import passiveprocessengine.definition.ArtifactType;
import impactassessment.api.Commands.*;
import impactassessment.api.Queries.*;

import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
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

import static impactassessment.general.IdGenerator.getNewId;
import static impactassessment.ui.Helpers.createComponent;
import static impactassessment.ui.Helpers.showOutput;

@Slf4j
@Route
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
public class MainView extends VerticalLayout {

    private CommandGateway commandGateway;
    private QueryGateway queryGateway;
    private Snapshotter snapshotter;
    private Replayer replayer;
    private WorkflowDefinitionRegistry registry;
    private FrontendPusher pusher;
    private JiraPoller jiraPoller;

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
    public void setPusher(FrontendPusher pusher) {
        this.pusher = pusher;
    }
    @Inject
    public void setJiraPoller(JiraPoller jiraPoller) {
        this.jiraPoller = jiraPoller;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        pusher.setUi(attachEvent.getUI());
        pusher.setView(this);
    }

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
        header.add(new Icon(VaadinIcon.CLUSTER), new Label(""), new Text("Process Dashboard"));

        HorizontalLayout footer = new HorizontalLayout();
        footer.setClassName("footer-theme");
        footer.setMargin(false);
        footer.setSizeFull();
        footer.setHeight("2%");
        footer.add(new Text("JKU ISSE - Stefan Bichler"));
        footer.setJustifyContentMode(JustifyContentMode.END);

        add(
                header,
                main(),
                footer
        );
    }

    private Component main() {
        HorizontalLayout main = new HorizontalLayout();
        main.setClassName("layout-style");
        main.setHeight("92%");
        main.add(menu(), content());
        return main;
    }

    private Component content() {
        Tab tab1 = new Tab("Current State");
        VerticalLayout cur = statePanel(false);
        cur.setHeight("100%");


        Tab tab2 = new Tab("Snapshot State");
        VerticalLayout snap = snapshotPanel(false);
        snap.setHeight("100%");
        snap.setVisible(false);

        Tab tab3 = new Tab("Compare");
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

        VerticalLayout content = new VerticalLayout();
        content.setClassName("layout-style");
        content.add(tabs, pages);
        return  content;
    }

    private Component menu() {
        VerticalLayout menu = new VerticalLayout();
        menu.addClassName("light-theme");
        menu.setPadding(true);
        menu.setMargin(false);
        menu.setWidth("35%");
        menu.setFlexGrow(0);

        Accordion accordion = new Accordion();
        accordion.add("Create Workflow", importArtifact());
        accordion.add("Mock Workflow", importMocked());
        accordion.add("Updates", updates());
//        accordion.add("Remove Workflow", remove()); // functionality provided via icon in the table
//        accordion.add("Evaluate Constraint", evaluate()); // functionality provided via icon in the table
        accordion.add("Backend Queries", backend());
        accordion.close();
        accordion.open(0);
        accordion.setWidthFull();

        menu.add(new H2("Controls"), accordion);

        return menu;
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
                List<WorkflowInstanceWrapper> response = future.get(5, TimeUnit.SECONDS).getState();
                grid.updateTreeGrid(response);
            } catch (TimeoutException e1) {
                log.error("GetStateQuery resulted in TimeoutException, make sure projection is initialized (Replay all Events first)!");
                Notification.show("Timeout: Replay all Events first..");
            } catch (InterruptedException | ExecutionException e2) {
                log.error("GetStateQuery resulted in Exception: "+e2.getMessage());
            }
        });
        Button replay = new Button("Replay All Events", evt -> {
            replayer.replay("projection");
            Notification.show("Replaying..");
        });

        controlButtonLayout.add(getState, replay);
        return controlButtonLayout;
    }

    private Component snapshotStateControls(WorkflowTreeGrid grid, ProgressBar progressBar) {
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
        layout.add(valueDatePicker);
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

        layout.add(valueDatePicker, snapshotButton, step, jump, stop);

        return layout;
    }


    private Component importArtifact() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setWidth("90%");

        // Process Definition
        RadioButtonGroup<String> processDefinition = new RadioButtonGroup<>();
        processDefinition.setItems(registry == null ? Collections.emptySet() : registry.getAll().keySet());
        processDefinition.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);


        Button loadDefinitions = new Button("Fetch Available Definitions", e -> {
            processDefinition.setItems(registry == null ? Collections.emptySet() : registry.getAll().keySet());
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
        processDefinition.addValueChangeListener( e -> {
            WorkflowDefinitionContainer wfdContainer = registry.get(e.getValue());
            source.removeAll();
            for (ArtifactType artT : wfdContainer.getWfd().getExpectedInput().values()) {
                TextField tf = new TextField();
                tf.setLabel("JIRA"/*artT.getArtifactType()*/); // TODO: remove hardcoded JIRA and set expected ArtifactType according to source
                source.add(tf);
            }
            if (wfdContainer.getWfd().getExpectedInput().size() == 0) {
                source.add(new Paragraph("no input artifacts are expected for this workflow.."));
            }
        });

        Button importOrUpdateArtifactButton = new Button("Create", evt -> {
            try {
                // collect all input IDs
                Map<String, String> inputs = new HashMap<>();
                source.getChildren()
                        .filter(child -> child instanceof TextField)
                        .map(child -> (TextField)child)
                        .filter(tf -> !tf.getValue().equals(""))
                        .filter(tf -> !tf.getLabel().equals(""))
                        .forEach(tf -> inputs.put(tf.getValue(), tf.getLabel()));
                // send command
                commandGateway.sendAndWait(new CreateWorkflowCmd(getNewId(), inputs, processDefinition.getValue()));
                Notification.show("Success");
            } catch (CommandExecutionException e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                log.error("CommandExecutionException: "+e.getMessage());
                Notification.show("Creation failed!");
            }
        });
        importOrUpdateArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);

        layout.add(
                new H4("1. Select Process Definition"),
                processDefinition,
                loadDefinitions,
                upload,
                addDefinition,
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

        Button print = new Button("PrintKBQuery");
        print.addClickListener(evt -> {
            queryGateway.query(new PrintKBQuery(id.getValue()), PrintKBResponse.class);
            Notification.show("Success");
        });

        //---------------------------------------------------------
        timer.setHeight("20px");
        timer.addClassName("big-text");
        timer.setVisible(false);
        TextField textField = new TextField();
        textField.setLabel("Update Interval in Minutes");
        textField.setValue("1");

        Checkbox checkbox = new Checkbox("Enable Automatic updates");
        checkbox.setValue(false);
        checkbox.addValueChangeListener(e -> {
            if (e.getValue()) {
                try {
                    int interval = Integer.parseInt(textField.getValue());
                    textField.setEnabled(false);
                    timer.setStartTime(new BigDecimal(interval*60));
                    timer.setVisible(true);
                    timer.start();
                    jiraPoller.setInterval(interval);
                    jiraPoller.start();
                } catch (NumberFormatException ex) {
                    Notification.show("Please enter a number");
                }
            } else {
                jiraPoller.interrupt();
                textField.setEnabled(true);
                timer.setVisible(false);
                timer.pause();
                timer.reset();
            }
        });
        //---------------------------------------------------------
        return new VerticalLayout(description, id, print, timer, textField, checkbox);
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

        Button importOrUpdateArtifactButton = new Button("Create Mock-Workflow", evt -> {
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

        layout.add(row1, row2);
        return layout;
    }

    private @Getter
    SimpleTimer timer = new SimpleTimer(60);
    private Component updates() {


        Button update = new Button("Fetch Updates Now", e -> {
                ChangeStreamPoller changeStreamPoller = SpringUtil.getBean(ChangeStreamPoller.class);
                Thread t = new Thread(changeStreamPoller);
                try {
                    t.start();
                    t.join();
                } catch (Exception ex) {
                    log.error("Update error: " + ex.getMessage());
                }
        });

        return new VerticalLayout(update);
    }

    private VerticalLayout snapshotPanel(boolean addHeader) {
        WorkflowTreeGrid grid = new WorkflowTreeGrid(x -> commandGateway.send(x), false);
        grid.initTreeGrid();
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");

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
