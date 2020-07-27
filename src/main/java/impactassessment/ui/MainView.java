package impactassessment.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.query.snapshot.Snapshotter;
import impactassessment.utils.Replayer;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Route
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
public class MainView extends VerticalLayout {

    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private QueryGateway queryGateway;
    @Autowired
    private Snapshotter snapshotter;
    @Autowired
    private Replayer replayer;

    private WorkflowTreeGrid stateGrid;
    private WorkflowTreeGrid snapshotGrid;

    public MainView() {
        setSizeFull();
        setMargin(false);
        setPadding(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setMargin(false);
        header.setPadding(true);
        header.setSizeFull();
        header.add(new H1("CQRS-based Quality Assurance Application - User Interface"));

        HorizontalLayout footer = new HorizontalLayout();
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
        main.setSizeFull();
        main.setPadding(false);
        main.setMargin(false);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setMargin(false);
        content.add(statePanel(), snapshotPanel());

        main.add(menu(), content);

        return main;
    }

    private Component menu() {
        VerticalLayout menu = new VerticalLayout();
        menu.setPadding(true);
        menu.setMargin(false);
        menu.setWidth("40%");
        menu.setFlexGrow(0);

        menu.add(
                new H3("Controls"),
                controlButtons(),
                snapshot(),
                accordion()
        );

        return menu;
    }

    private Component controlButtons() {
        HorizontalLayout controlButtonLayout = new HorizontalLayout();
        controlButtonLayout.setMargin(false);
        controlButtonLayout.setPadding(false);
        controlButtonLayout.setWidthFull();

        Button getState = new Button("Update State");
        getState.addClickListener(evt -> {
            CompletableFuture<GetStateResponse> future = queryGateway.query(new GetStateQuery(0), GetStateResponse.class);
            try {
                List<WorkflowInstanceWrapper> response = future.get().component1();
                stateGrid.updateTreeGrid(response);
            } catch (InterruptedException | ExecutionException e) {
                log.error("GetStateQuery resulted in InterruptedException or ExecutionException: "+e.getMessage());
            }
        });
        Button replay = new Button("Replay Events");
        replay.addClickListener(evt -> {
            replayer.replay("projection");
            Notification.show("Replaying..");
        });

        controlButtonLayout.add(getState, replay);
        return controlButtonLayout;
    }

    private Component snapshot() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setMargin(false);
        layout.setPadding(false);
        // Date Picker
        DatePicker valueDatePicker = new DatePicker();
        LocalDate now = LocalDate.now();
        valueDatePicker.setValue(now);
        valueDatePicker.setLabel("Date");
        // Time Picker
        HorizontalLayout timePicker = new HorizontalLayout();
        timePicker.setWidthFull();
        timePicker.setMargin(false);
        timePicker.setPadding(false);
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
        timePicker.add(hour, min, sec);
        // Snapshot Button
        Button snapshotButton = new Button("Snapshot");
        snapshotButton.addClickListener(evt -> {
            LocalDateTime snapshotTime = LocalDateTime.of(valueDatePicker.getValue().getYear(),
                    valueDatePicker.getValue().getMonth().getValue(),
                    valueDatePicker.getValue().getDayOfMonth(),
                    hour.getValue().intValue(),
                    min.getValue().intValue(),
                    sec.getValue().intValue());
            Future<Map<String, WorkflowInstanceWrapper>> future = snapshotter.replayEventsUntil(snapshotTime.atZone(ZoneId.systemDefault()).toInstant());
            try {
                List<WorkflowInstanceWrapper> response = future.get().entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList());
                snapshotGrid.updateTreeGrid(response);
            } catch (InterruptedException | ExecutionException e) {
                log.error("GetStateQuery resulted in InterruptedException or ExecutionException: "+e.getMessage());
            }
        });

        layout.add(valueDatePicker, timePicker, snapshotButton);

        return layout;
    }

    private Component accordion() {
        Accordion accordion = new Accordion();
        accordion.add("Import Artifact", importArtifact());
        accordion.add("Import Mock-Artifact", importMockArtifact());
        accordion.add("Evaluate Constraint", evaluate());
        accordion.add("Backend commands", backend());
        accordion.close();
        accordion.setWidthFull();
        return accordion;
    }

    private Component importArtifact() {
        ComboBox<String> valueComboBox = new ComboBox<>();
        valueComboBox.setItems(Sources.JIRA.toString(), "more coming soon..");
        valueComboBox.setValue(Sources.JIRA.toString());
        valueComboBox.setAllowCustomValue(false);
        valueComboBox.setLabel("Source");

        TextField id = new TextField("Key");
        id.setValue("11320"); //similar issue: "11321", Hazard with links: "11661"

        Button add = new Button("Import Artifact");
        add.addClickListener(evt -> {
            commandGateway.send(new AddArtifactCmd(id.getValue(), Sources.valueOf(valueComboBox.getValue())));
        });

        Button update = new Button("Update Artifact");
        update.addClickListener(evt -> {
            commandGateway.send(new UpdateArtifactCmd(id.getValue(), Sources.valueOf(valueComboBox.getValue())));
        });

        HorizontalLayout layout1 = new HorizontalLayout();
        layout1.setMargin(false);
        layout1.setPadding(false);
        layout1.setWidthFull();
        layout1.add(valueComboBox, id);
        layout1.setAlignItems(Alignment.END);

        HorizontalLayout layout2 = new HorizontalLayout();
        layout2.setMargin(false);
        layout2.setPadding(false);
        layout2.setWidthFull();
        layout2.add(add, update);
        layout2.setAlignItems(Alignment.END);

        return new VerticalLayout(layout1, layout2);
    }

    private Component importMockArtifact() {
        TextField id = new TextField("ID");
        id.setValue("A3");

        TextField status = new TextField("Status");
        status.setValue(JiraMockService.DEFAULT_STATUS);

        TextField issuetype = new TextField("Issue-Type");
        issuetype.setValue(JiraMockService.DEFAULT_ISSUETYPE);

        TextField priority = new TextField("Priority");
        priority.setValue(JiraMockService.DEFAULT_PRIORITY);

        TextField summary = new TextField("Summary");
        summary.setValue(JiraMockService.DEFAULT_SUMMARY);
        summary.setWidthFull();

        Button add = new Button("Add Artifact");
        add.addClickListener(evt -> {
            IJiraArtifact a = JiraMockService.mockArtifact(id.getValue(), status.getValue(), issuetype.getValue(), priority.getValue(), summary.getValue());
            commandGateway.sendAndWait(new AddMockArtifactCmd(id.getValue(), a));
            Notification.show("Success");
        });

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
        row1.setMargin(false);
        row1.setPadding(false);

        VerticalLayout row2 = new VerticalLayout();
        row2.setMargin(false);
        row2.setPadding(false);
        row2.add(summary, add);

        return new VerticalLayout(row1, row2);
    }

    private Component evaluate() {
        TextField id = new TextField("ID");
        id.setValue("A3");

        TextField corr = new TextField("Corr");
        corr.setValue("4_open_A3");

        Button check = new Button("Check");
        check.addClickListener(evt -> {
            commandGateway.sendAndWait(new CheckConstraintCmd(id.getValue(), corr.getValue()));
            Notification.show("Success");
        });

        HorizontalLayout row = new HorizontalLayout();
        row.setMargin(false);
        row.setPadding(false);
        row.setWidthFull();
        row.add(id, corr);
        row.setAlignItems(Alignment.END);

        return new VerticalLayout(row, check);
    }

    private Component backend() {
        TextField id = new TextField("ID");
        id.setValue("A3");

        Button delete = new Button("send DeleteCmd");
        delete.addClickListener(evt -> {
            commandGateway.send(new DeleteCmd(id.getValue()));
        });

        Button print = new Button("send PrintKBCmd");
        print.addClickListener(evt -> {
            commandGateway.sendAndWait(new PrintKBCmd(id.getValue()));
            Notification.show("Success");
        });

        Button query = new Button("send FindQuery");
        query.addClickListener(evt -> {
            CompletableFuture<FindResponse> val = queryGateway.query(new FindQuery(id.getValue()), FindResponse.class);
            try {
                Notification.show(String.valueOf(val.get().getAmount()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return new VerticalLayout(id, delete, print, query);
    }

    private VerticalLayout snapshotPanel() {
        snapshotGrid = new WorkflowTreeGrid();
        snapshotGrid.initTreeGrid();
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.add(new H2("Snapshot State"), snapshotGrid);
        return layout;
    }

    private VerticalLayout statePanel() {
        stateGrid = new WorkflowTreeGrid();
        stateGrid.initTreeGrid();
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.add(new H2("Current State"), stateGrid);
        return layout;
    }


}
