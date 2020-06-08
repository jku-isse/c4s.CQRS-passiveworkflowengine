package impactassessment.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Route;
import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.query.MockDatabase;
import impactassessment.query.snapshot.Snapshotter;
import impactassessment.utils.Replayer;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Route
@Push
@Profile("ui")
@XSlf4j
public class WebUI extends VerticalLayout {

    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private QueryGateway queryGateway;
    @Autowired
    private Snapshotter snapshotter;
    @Autowired
    private Replayer replayer;

    public WebUI() {
        Component commandPanel = commandPanel();
        Component checkPanel = checkPanel();
        Component printPanel = printPanel();

        Component replayPanel = replayPanel();
        Component snapshotPanel = snapshot();
        Component queryPanel = queryPanel();

        Component treeGrid = treeGrid();

        HorizontalLayout line1 = new HorizontalLayout();
        line1.setAlignItems(Alignment.CENTER);
        line1.add(commandPanel, checkPanel, printPanel);
        line1.setSizeFull();

        HorizontalLayout line2 = new HorizontalLayout();
        line2.setAlignItems(Alignment.CENTER);
        line2.add(replayPanel, snapshotPanel, queryPanel);
        line2.setSizeFull();

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.add(line1, line2, treeGrid);

        add(verticalLayout);
    }

    private Component commandPanel() {
        TextField id = new TextField("ID");
        id.setLabel("A3");
        TextField status = new TextField("Status");
        status.setValue(MockService.DEFAULT_STATUS);
        TextField issuetype = new TextField("Issue-Type");
        issuetype.setValue(MockService.DEFAULT_ISSUETYPE);
        TextField priority = new TextField("Priority");
        priority.setValue(MockService.DEFAULT_PRIORITY);
        TextField summary = new TextField("Summary");
        summary.setSizeFull();
        summary.setValue(MockService.DEFAULT_SUMMARY);

        Button add = new Button("Add Artifact");

        add.addClickListener(evt -> {
            Artifact a = MockService.mockArtifact(id.getValue(), status.getValue(), issuetype.getValue(), priority.getValue(), summary.getValue());
            commandGateway.sendAndWait(new AddArtifactCmd(id.getValue(), a));
            Notification.show("Success");
        });

        FormLayout form = new FormLayout();
        form.add(id, status, issuetype, priority, summary, add);
        return form;
    }

    private Component queryPanel() {
        TextField id = new TextField("ID");
        id.setLabel("A3");
        Button query = new Button("Query");

        query.addClickListener(evt -> {
            CompletableFuture<FindResponse> val = queryGateway.query(new FindQuery(id.getValue()), FindResponse.class);
            try {
                Notification.show(String.valueOf(val.get().getAmount()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        FormLayout form = new FormLayout();
        form.add(id, query);
        return form;
    }

    private Component checkPanel() {
        TextField id = new TextField("ID");
        id.setLabel("A3");
        TextField corr = new TextField("Corr");
        corr.setLabel("4_open_A3");
        Button check = new Button("Check");
        Button print = new Button("Print KB");

        check.addClickListener(evt -> {
            commandGateway.sendAndWait(new CheckConstraintCmd(id.getValue(), corr.getValue()));
            Notification.show("Success");
        });
        print.addClickListener(evt -> {
            commandGateway.sendAndWait(new PrintKBCmd(id.getValue()));
            Notification.show("Success");
        });

        FormLayout form = new FormLayout();
        form.add(id, corr, check, print);
        return form;
    }

    private Component printPanel() {
        TextField id = new TextField("ID");
        id.setValue("A3");
        Button print = new Button("Print KB");

        print.addClickListener(evt -> {
            commandGateway.sendAndWait(new PrintKBCmd(id.getValue()));
            Notification.show("Success");
        });

        FormLayout form = new FormLayout();
        form.add(id, print);
        return form;
    }

    private Component snapshot() {
        TextField snapshotTimestamp = new TextField("Timestamp");
        snapshotTimestamp.setLabel(Instant.now().toString());
        snapshotTimestamp.setSizeFull();

        Button snapshot = new Button("Snapshot");

        snapshot.addClickListener(evt -> {
            Future<MockDatabase> future = snapshotter.replayEventsUntil(Instant.parse(snapshotTimestamp.getValue()));
        });

        FormLayout form = new FormLayout();
        form.add(snapshotTimestamp, snapshot);
        return form;
    }

    private Component replayPanel() {
        Button replay = new Button("Start Replay");
        replay.addClickListener(evt -> {
            replayer.replay("projection");
            Notification.show("Replaying..");
        });

        FormLayout form = new FormLayout();
        form.add(replay);
        return form;
    }

    private Component treeGrid() {
        Button getState = new Button("Get State");
        TreeGrid<WorkflowInstanceWrapper> grid = new TreeGrid<>();

        getState.addClickListener(evt -> {
            CompletableFuture<GetStateResponse> future = queryGateway.query(new GetStateQuery(0), GetStateResponse.class);
            try {
                List<WorkflowInstanceWrapper> response = future.get().component1();
                grid.setItems(response);
                grid.addHierarchyColumn(wfi -> wfi.getWorkflowInstance().getWorkflow().getId());
            } catch (InterruptedException | ExecutionException e) {
               e.printStackTrace();
            }
        });

        VerticalLayout layout = new VerticalLayout();
        layout.add(getState, grid);
        return layout;
    }
}
