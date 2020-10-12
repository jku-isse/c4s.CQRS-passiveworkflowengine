package impactassessment.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.Route;
import impactassessment.api.*;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.query.Snapshotter;
import impactassessment.query.Replayer;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.ProcessDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static impactassessment.ui.Helpers.createComponent;
import static impactassessment.ui.Helpers.showOutput;

@Slf4j
@Route
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
public class MainView extends VerticalLayout {

    private CommandGateway commandGateway;
    private QueryGateway queryGateway;
    private Snapshotter snapshotter;
    private Replayer replayer;
    private ProcessDefinitionRegistry registry;

    private WorkflowTreeGrid stateGrid;
    private WorkflowTreeGrid snapshotGrid;

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
    public void setProcessDefinitionRegistry(ProcessDefinitionRegistry registry) {
        this.registry = registry;
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
        header.add(new Icon(VaadinIcon.AUTOMATION), new Label(""), new Text("Workflow Monitoring Tool for Software Development Artifacts"));

        HorizontalLayout footer = new HorizontalLayout();
        footer.setClassName("footer-theme");
        footer.setMargin(false);
        footer.setSizeFull();
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

        VerticalLayout content = new VerticalLayout();
        content.setClassName("layout-style");
        content.add(statePanel(), snapshotPanel());

        main.add(menu(), content);

        return main;
    }

    private Component menu() {
        VerticalLayout menu = new VerticalLayout();
        menu.addClassName("light-theme");
        menu.setPadding(true);
        menu.setMargin(false);
        menu.setWidth("35%");
        menu.setFlexGrow(0);

        Accordion accordion = new Accordion();
        accordion.add("Import Artifact", importArtifact());
        accordion.add("Import Mock-Artifact", importMocked());
        accordion.add("Remove Artifact", remove());
//        accordion.add("Evaluate Constraint", evaluate()); // evaluating now over icon-buttons in current-state-grid
        accordion.add("Backend Queries", backend());
        accordion.close();
        accordion.open(0);
        accordion.setWidthFull();

        menu.add(new H2("Controls"), accordion);

        return menu;
    }

    private Component currentStateControls() {
        HorizontalLayout controlButtonLayout = new HorizontalLayout();
        controlButtonLayout.setMargin(false);
        controlButtonLayout.setPadding(false);
        controlButtonLayout.setWidthFull();

        Button getState = new Button("Get State");
        getState.addClickListener(evt -> {
            CompletableFuture<GetStateResponse> future = queryGateway.query(new GetStateQuery(0), GetStateResponse.class);
            try {
                List<WorkflowInstanceWrapper> response = future.get().component1();
                stateGrid.updateTreeGrid(response);
            } catch (InterruptedException | ExecutionException e) {
                log.error("GetStateQuery resulted in InterruptedException or ExecutionException: "+e.getMessage());
            }
        });
        Button replay = new Button("Replay All Events");
        replay.addClickListener(evt -> {
            replayer.replay("projection");
            Notification.show("Replaying..");
        });

        controlButtonLayout.add(getState, replay);
        return controlButtonLayout;
    }

    private Component snapshotStateControls() {
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
                snapshotGrid.updateTreeGrid(snapshotter.getState());
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
                snapshotGrid.updateTreeGrid(snapshotter.getState());
                progressBar.setValue(snapshotter.getProgress());
            } else {
                Notification.show("Specified time is after the last or before the first event!");
            }
        });
        jump.setEnabled(false);

        stop.addClickListener(e -> {
            snapshotter.quit();
            progressBar.setValue(0);
            snapshotGrid.updateTreeGrid(Collections.emptyList());
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
                snapshotGrid.updateTreeGrid(snapshotter.getState());
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
        processDefinition.setLabel("1. Select Process Definition");
        processDefinition.setItems(registry == null ? Collections.emptySet() : registry.getDefinitions().keySet());
        processDefinition.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

        Button loadDefinitions = new Button("Load Available Definitions", e -> {
            processDefinition.setItems(registry == null ? Collections.emptySet() : registry.getDefinitions().keySet());
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

        Button addDefinition = new Button("Add New Definition", e -> {
            try {
                List<String> ruleFiles = new ArrayList<>();
                String json = null;
                String name = null;
                for (String filename : buffer.getFiles()) {
                    if (filename.endsWith(".json")) {
                        json = IOUtils.toString(buffer.getInputStream(filename), StandardCharsets.UTF_8.name());
                        name = filename.replace(".json", "");
                    } else if (filename.endsWith(".drl")) {
                        ruleFiles.add(IOUtils.toString(buffer.getInputStream(filename), StandardCharsets.UTF_8.name()));
                    } else {
                        // not allowed
                    }
                }
                if (json != null && ruleFiles.size() > 0) {
                    registry.register(name, json, ruleFiles);
                    processDefinition.setItems(registry == null ? Collections.emptySet() : registry.getDefinitions().keySet());
                    Notification.show("Workflow loaded and added to registry");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Source

        RadioButtonGroup<String> source = new RadioButtonGroup<>();
        source.setLabel("2. Select Source");
        source.setItems(Sources.JIRA.toString(), "JAMA", "GitHub");
        source.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        source.setValue(Sources.JIRA.toString());
        source.setItemEnabledProvider(item -> !(item.equals("JAMA")||item.equals("GitHub")));

        // Key

        TextField key = new TextField("3. Specify Key");
        key.setValue("UAV-1117");

        Button importOrUpdateArtifactButton = new Button("Import or Update Artifact", evt -> {
            try {
                commandGateway.sendAndWait(new ImportOrUpdateArtifactWithWorkflowDefinitionCmd(key.getValue(), Sources.valueOf(source.getValue()), processDefinition.getValue()));
                Notification.show("Success");
            } catch (CommandExecutionException e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                log.error("CommandExecutionException: "+e.getMessage());
                Notification.show("Import failed!");
            }
        });
        importOrUpdateArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);

        layout.add(processDefinition, loadDefinitions, upload, addDefinition, source, key, importOrUpdateArtifactButton);
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

        return new VerticalLayout(description, id, print);
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

        Button importOrUpdateArtifactButton = new Button("Import or Update Mock-Artifact", evt -> {
            try {
                commandGateway.sendAndWait(new AddMockArtifactCmd(id.getValue(), status.getValue(), issuetype.getValue(), priority.getValue(), summary.getValue()));
                Notification.show("Success");
            } catch (CommandExecutionException e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                log.error("CommandExecutionException: "+e.getMessage());
                Notification.show("Import failed!");
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
    private VerticalLayout snapshotPanel() {
        snapshotGrid = new WorkflowTreeGrid(x -> commandGateway.send(x), false);
        snapshotGrid.initTreeGrid();
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.add(
                new Text("Snapshot State"),
                progress(),
                snapshotGrid,
                snapshotStateControls()
        );
        return layout;
    }

    private ProgressBar progressBar;
    private Component progress() {
        progressBar = new ProgressBar();
        progressBar.setValue(0);
        return progressBar;
    }
    private VerticalLayout statePanel() {
        stateGrid = new WorkflowTreeGrid(x -> commandGateway.send(x), true);
        stateGrid.initTreeGrid();
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.add(
                new Text("Current State"),
                stateGrid,
                currentStateControls()
        );
        return layout;
    }

}
