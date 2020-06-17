package impactassessment.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Route;
import impactassessment.api.*;
import impactassessment.artifact.mock.MockService;
import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.model.definition.QACheckDocument;
import impactassessment.model.definition.RuleEngineBasedConstraint;
import impactassessment.model.workflowmodel.IdentifiableObject;
import impactassessment.model.workflowmodel.WorkflowInstance;
import impactassessment.model.workflowmodel.WorkflowTask;
import impactassessment.query.snapshot.Snapshotter;
import impactassessment.utils.Replayer;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalField;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Route
public class MainView extends VerticalLayout {

    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private QueryGateway queryGateway;
    @Autowired
    private Snapshotter snapshotter;
    @Autowired
    private Replayer replayer;

    private TreeGrid<IdentifiableObject> stateGrid;
    private TreeGrid<IdentifiableObject> snapshotGrid;

    public MainView() {
        setSizeFull();
        setMargin(false);
        setPadding(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setMargin(false);
        header.setPadding(true);
        header.setSizeFull();
        header.add(new H1("CQRS based Workflow-Engine"));

        HorizontalLayout main = new HorizontalLayout();
        main.setSizeFull();
        main.setPadding(false);
        main.setMargin(false);

        VerticalLayout menu = new VerticalLayout();
        menu.setPadding(true);
        menu.setMargin(false);
        menu.setWidth("40%");
        menu.setFlexGrow(0);

        Button replay = new Button("Replay Events");
        replay.addClickListener(evt -> {
            replayer.replay("projection");
            Notification.show("Replaying..");
        });
        Button getState = new Button("Update State");
        getState.addClickListener(evt -> {
            CompletableFuture<GetStateResponse> future = queryGateway.query(new GetStateQuery(0), GetStateResponse.class);
            try {
                List<WorkflowInstanceWrapper> response = future.get().component1();
                updateTreeGrid(stateGrid, response);
            } catch (InterruptedException | ExecutionException e) {
                log.error("GetStateQuery resulted in InterruptedException or ExecutionException: "+e.getMessage());
            }
        });

        Instant time = Instant.now();

        DatePicker valueDatePicker = new DatePicker();
        LocalDate now = LocalDate.now();
        valueDatePicker.setValue(now);
        valueDatePicker.setLabel("Date");

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

        TextField snapshotTimestamp = new TextField("Timestamp");
        snapshotTimestamp.setValue(Instant.now().toString());
        snapshotTimestamp.setWidthFull();


        Button snapshot = new Button("Snapshot");
        snapshot.addClickListener(evt -> {
            LocalDateTime snapshotTime = LocalDateTime.of(valueDatePicker.getValue().getYear(),
                    valueDatePicker.getValue().getMonth().getValue(),
                    valueDatePicker.getValue().getDayOfMonth(),
                    hour.getValue().intValue(),
                    min.getValue().intValue(),
                    sec.getValue().intValue());
            Future<Map<String, WorkflowInstanceWrapper>> future = snapshotter.replayEventsUntil(snapshotTime.atZone(ZoneId.systemDefault()).toInstant());
            try {
                List<WorkflowInstanceWrapper> response = future.get().entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList());
                updateTreeGrid(snapshotGrid, response);
            } catch (InterruptedException | ExecutionException e) {
                log.error("GetStateQuery resulted in InterruptedException or ExecutionException: "+e.getMessage());
            }
        });
        HorizontalLayout snap1 = new HorizontalLayout();
        snap1.setWidthFull();
        snap1.setMargin(false);
        snap1.setPadding(false);
        snap1.add(valueDatePicker);
        snap1.setAlignItems(Alignment.END);
        HorizontalLayout snap2 = new HorizontalLayout();
        snap2.setWidthFull();
        snap2.setMargin(false);
        snap2.setPadding(false);
        snap2.add(hour, min, sec);

        Accordion accordion = new Accordion();
        accordion.add("Import Artifact", addPanel());
        accordion.add("Import Mock-Artifact", commandPanel());
        accordion.add("Evaluate Constraint", checkPanel());
        accordion.add("Backend commands", backendPanel());
        accordion.close();
        accordion.setWidthFull();

        HorizontalLayout h = new HorizontalLayout();
        h.setMargin(false);
        h.setPadding(false);
        h.setWidthFull();
        h.add(getState, replay);

        menu.add(
                new H3("Controls"),
                h,
                snap1,
                snap2,
                snapshot,
                accordion
        );

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setMargin(false);
        content.add(replayPanel(), snapshotPanel());

        HorizontalLayout footer = new HorizontalLayout();
        footer.setSizeFull();
        footer.add(new Text("JKU ISSE - Stefan Bichler"));
        footer.setJustifyContentMode(JustifyContentMode.END);

        main.add(
                menu,
                content
        );

        add(
                header,
                main,
                footer
        );
    }

    private HorizontalLayout backendPanel() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(false);
        layout.setPadding(true);
        layout.add(
                printPanel(),
                queryPanel()
        );
        return layout;
    }

    private VerticalLayout commandPanel() {
        TextField id = new TextField("ID");
        id.setValue("A3");
        TextField status = new TextField("Status");
        status.setValue(MockService.DEFAULT_STATUS);
        TextField issuetype = new TextField("Issue-Type");
        issuetype.setValue(MockService.DEFAULT_ISSUETYPE);
        TextField priority = new TextField("Priority");
        priority.setValue(MockService.DEFAULT_PRIORITY);
        TextField summary = new TextField("Summary");
        summary.setValue(MockService.DEFAULT_SUMMARY);
        summary.setWidthFull();

        Button add = new Button("Add Artifact");

//        add.addClickListener(evt -> {
//            IArtifact a = MockService.mockArtifact(id.getValue(), status.getValue(), issuetype.getValue(), priority.getValue(), summary.getValue());
//            commandGateway.sendAndWait(new AddMockArtifactCmd(id.getValue(), a));
//            Notification.show("Success");
//        });

        VerticalLayout v1 = new VerticalLayout();
        v1.add(id, status);
        v1.setWidth("50%");
        VerticalLayout v2 = new VerticalLayout();
        v2.add(issuetype, priority);
        v2.setWidth("50%");
        HorizontalLayout h = new HorizontalLayout(v1, v2);
        VerticalLayout v3 = new VerticalLayout();
        v3.add(summary, add);

        v1.setMargin(false);
        v1.setPadding(false);
        v2.setMargin(false);
        v2.setPadding(false);
        v3.setMargin(false);
        v3.setPadding(false);
        h.setMargin(false);
        h.setPadding(false);

        return new VerticalLayout(h, v3);
    }

    private VerticalLayout addPanel() {
        ComboBox<String> valueComboBox = new ComboBox<>();
        valueComboBox.setItems(Sources.JIRA.toString(), "more coming soon..");
        valueComboBox.setValue(Sources.JIRA.toString());
        valueComboBox.setAllowCustomValue(false);
        valueComboBox.setLabel("Source");

        TextField id = new TextField("Key");
        id.setValue("11320");
        Button add = new Button("Import Artifact");
        add.addClickListener(evt -> {
            commandGateway.send(new AddArtifactCmd(id.getValue(), Sources.valueOf(valueComboBox.getValue())));
        });
        Button update = new Button("Update Artifact");
        update.addClickListener(evt -> {
            commandGateway.send(new UpdateArtifactCmd(id.getValue(), Sources.valueOf(valueComboBox.getValue())));
        });

        HorizontalLayout h = new HorizontalLayout();
        h.setMargin(false);
        h.setPadding(false);
        h.setWidthFull();
        h.add(valueComboBox, id);
        h.setAlignItems(Alignment.END);

        HorizontalLayout h2 = new HorizontalLayout();
        h2.setMargin(false);
        h2.setPadding(false);
        h2.setWidthFull();
        h2.add(add, update);
        h2.setAlignItems(Alignment.END);

        return new VerticalLayout(h, h2);
    }

    private VerticalLayout queryPanel() {
        TextField id = new TextField("ID");
        id.setValue("A3");
        Button query = new Button("Find");

        query.addClickListener(evt -> {
            CompletableFuture<FindResponse> val = queryGateway.query(new FindQuery(id.getValue()), FindResponse.class);
            try {
                Notification.show(String.valueOf(val.get().getAmount()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        VerticalLayout form = new VerticalLayout();
        form.add(new H5("send FindQuery"), id, query);
        return form;
    }

    private VerticalLayout checkPanel() {
        TextField id = new TextField("ID");
        id.setValue("A3");
        TextField corr = new TextField("Corr");
        corr.setValue("4_open_A3");
        Button check = new Button("Check");

        check.addClickListener(evt -> {
            commandGateway.sendAndWait(new CheckConstraintCmd(id.getValue(), corr.getValue()));
            Notification.show("Success");
        });

        HorizontalLayout h = new HorizontalLayout();
        h.setMargin(false);
        h.setPadding(false);
        h.setWidthFull();
        h.add(id, corr);
        h.setAlignItems(Alignment.END);

        VerticalLayout form = new VerticalLayout();
        form.add(h, check);
        return form;
    }

    private VerticalLayout printPanel() {
        TextField id = new TextField("ID");
        id.setValue("A3");
        Button print = new Button("Print");

        print.addClickListener(evt -> {
            commandGateway.sendAndWait(new PrintKBCmd(id.getValue()));
            Notification.show("Success");
        });

        VerticalLayout form = new VerticalLayout();
        form.add(new H5("send PrintKBCmd"), id, print);
        return form;
    }

    private VerticalLayout snapshotPanel() {
        snapshotGrid = new TreeGrid<>();
        initTreeGrid(snapshotGrid);

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.add(new H2("Snapshot State"), snapshotGrid);
        return layout;
    }

    private VerticalLayout replayPanel() {
        stateGrid = new TreeGrid<>();
        initTreeGrid(stateGrid);

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.add(new H2("Current State"), stateGrid);
        return layout;
    }

    private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

    private TreeGrid<IdentifiableObject> initTreeGrid(TreeGrid<IdentifiableObject> grid) {
        grid.addHierarchyColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getConstraintType();
            } else {
                return o.getClass().getSimpleName() + " - " + o.getId();
            }
        }).setHeader("Workflow Instance")
                .setWidth("40%");
        grid.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return formatter.format(rebc.getLastEvaluated());
            } else {
                return "";
            }
        }).setHeader("Last Evaluated");
        grid.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return formatter.format(rebc.getLastChanged());
            } else {
                return "";
            }
        }).setHeader("Last Changed");
        grid.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getFulfilledForReadOnly().stream().map(rl -> rl.getTitle() + " ").collect(Collectors.joining());
            } else {
                return "";
            }
        }).setHeader("Fulfilled");
        grid.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getUnsatisfiedForReadOnly().stream().map(rl -> rl.getTitle() + " ").collect(Collectors.joining());
            } else {
                return "";
            }
        }).setHeader("Unsatisfied");
        return grid;
    }

    private void updateTreeGrid(TreeGrid<IdentifiableObject> grid, List<WorkflowInstanceWrapper> content) {
        if (content != null) {
            grid.setItems(content.stream().map(WorkflowInstanceWrapper::getWorkflowInstance), o -> {
                if (o instanceof WorkflowInstance) {
                    WorkflowInstance wfi = (WorkflowInstance) o;
                    return wfi.getWorkflowTasksReadonly().stream().map(wft -> (IdentifiableObject) wft);
                } else if (o instanceof WorkflowTask) {
                    WorkflowTask wft = (WorkflowTask) o;
                    return wft.getOutput().stream().filter(ao -> ao.getArtifact() instanceof QACheckDocument).map(x -> (IdentifiableObject) x.getArtifact());
                } else if (o instanceof QACheckDocument) {
                    QACheckDocument qacd = (QACheckDocument) o;
                    return qacd.getConstraintsReadonly().stream().map(x -> (IdentifiableObject) x);
                } else if (o instanceof RuleEngineBasedConstraint) {
                    return Stream.empty();
                } else {
                    log.error("TreeGridPanel got unknown artifact: " + o.getClass().getSimpleName());
                    return Stream.empty();
                }
            });
            grid.getDataProvider().refreshAll();
        }
    }
}
