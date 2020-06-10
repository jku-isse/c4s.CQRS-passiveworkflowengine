package impactassessment.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Route;
import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import impactassessment.mock.artifact.jira.JiraService;
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

import java.io.IOException;
import java.time.Instant;
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
    private JiraService jira = new JiraService();

    public MainView() {
        HorizontalLayout line1 = new HorizontalLayout();
        line1.add(new VerticalLayout(commandPanel()), new VerticalLayout(jiraPanel()), new VerticalLayout(checkPanel()), new VerticalLayout(printPanel(), queryPanel()));
        line1.setSizeFull();

        VerticalLayout line2 = new VerticalLayout();
        line2.add(replayPanel(), snapshotPanel());
        line2.setSizeFull();

        add(
                new H1("CQRS Command/Query User Interface"),
                line1,
                line2
        );
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

        add.addClickListener(evt -> {
            Artifact a = MockService.mockArtifact(id.getValue(), status.getValue(), issuetype.getValue(), priority.getValue(), summary.getValue());
            commandGateway.sendAndWait(new AddArtifactCmd(id.getValue(), a));
            Notification.show("Success");
        });

        VerticalLayout v1 = new VerticalLayout();
        v1.add(id, status);
        VerticalLayout v2 = new VerticalLayout();
        v2.add(issuetype, priority);
        HorizontalLayout h = new HorizontalLayout(v1, v2);
        return new VerticalLayout(new H2("Send Add-Command"), h, new VerticalLayout(summary, add));
    }

    private VerticalLayout jiraPanel() {
        TextField id = new TextField("Jira Key");
        id.setValue("10076");
        Button add = new Button("Import Jira Artifact");
        add.addClickListener(evt -> {
            String issue = jira.get(id.getValue());
            log.info(issue);
        });
        return new VerticalLayout(new H2("Add from JIRA"), id, add);
    }

    private VerticalLayout queryPanel() {
        TextField id = new TextField("ID");
        id.setValue("A3");
        Button query = new Button("Query");

        query.addClickListener(evt -> {
            CompletableFuture<FindResponse> val = queryGateway.query(new FindQuery(id.getValue()), FindResponse.class);
            try {
                Notification.show(String.valueOf(val.get().getAmount()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        VerticalLayout form = new VerticalLayout();
        form.add(new H2("Find Query"), id, query);
        return form;
    }

    private VerticalLayout checkPanel() {
        TextField id = new TextField("ID");
        id.setValue("A3");
        TextField corr = new TextField("Corr");
        corr.setValue("4_open_A3");
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

        VerticalLayout form = new VerticalLayout();
        form.add(new H2("Check Constraint"), id, corr, check, print);
        return form;
    }

    private VerticalLayout printPanel() {
        TextField id = new TextField("ID");
        id.setValue("A3");
        Button print = new Button("Print KB");

        print.addClickListener(evt -> {
            commandGateway.sendAndWait(new PrintKBCmd(id.getValue()));
            Notification.show("Success");
        });

        VerticalLayout form = new VerticalLayout();
        form.add(new H2("PrintKBCmd"), id, print);
        return form;
    }

    private VerticalLayout snapshotPanel() {
        TextField snapshotTimestamp = new TextField("Timestamp");
        snapshotTimestamp.setValue(Instant.now().toString());
        snapshotTimestamp.setWidthFull();
        Button snapshot = new Button("Snapshot");
        TreeGrid<IdentifiableObject> grid = new TreeGrid<>();
        initTreeGrid(grid);

        snapshot.addClickListener(evt -> {
            Future<Map<String, WorkflowInstanceWrapper>> future = snapshotter.replayEventsUntil(Instant.parse(snapshotTimestamp.getValue()));
            try {
                List<WorkflowInstanceWrapper> response = future.get().entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList());
                updateTreeGrid(grid, response);
            } catch (InterruptedException | ExecutionException e) {
                log.error("GetStateQuery resulted in InterruptedException or ExecutionException: "+e.getMessage());
            }
        });

        VerticalLayout form = new VerticalLayout();
        form.add(new H2("Make Snapshot"), snapshotTimestamp, snapshot, grid);
        return form;
    }

    private VerticalLayout replayPanel() {
        Button replay = new Button("Start Replay");
        Button getState = new Button("Get State");
        TreeGrid<IdentifiableObject> grid = new TreeGrid<>();
        initTreeGrid(grid);

        replay.addClickListener(evt -> {
            replayer.replay("projection");
            Notification.show("Replaying..");
        });

        getState.addClickListener(evt -> {
            CompletableFuture<GetStateResponse> future = queryGateway.query(new GetStateQuery(0), GetStateResponse.class);
            try {
                List<WorkflowInstanceWrapper> response = future.get().component1();
                updateTreeGrid(grid, response);
            } catch (InterruptedException | ExecutionException e) {
               log.error("GetStateQuery resulted in InterruptedException or ExecutionException: "+e.getMessage());
            }
        });

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H2("Workflow State"), replay, getState, grid);
        return layout;
    }

    private TreeGrid<IdentifiableObject> initTreeGrid(TreeGrid<IdentifiableObject> grid) {
        grid.addHierarchyColumn(o -> o.getClass().getSimpleName() + " - " + o.getId())
                .setHeader("Workflow Instance")
                .setAutoWidth(true);
        grid.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getLastEvaluated();
            } else {
                return "";
            }
        }).setHeader("Last Evaluated");
        grid.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getLastChanged();
            } else {
                return "";
            }
        }).setHeader("Last Changed");
        grid.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getFulfilledForReadOnly().stream().map(rl -> rl.getId() + " ").collect(Collectors.joining());
            } else {
                return "";
            }
        }).setHeader("Fulfilled");
        grid.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getUnsatisfiedForReadOnly().stream().map(rl -> rl.getId() + " ").collect(Collectors.joining());
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
