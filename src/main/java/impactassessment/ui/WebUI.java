package impactassessment.ui;

import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import impactassessment.model.definition.WPManagementWorkflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.queryhandling.QueryGateway;
import impactassessment.api.*;
import com.vaadin.annotations.Push;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import impactassessment.query.snapshot.Snapshotter;
import impactassessment.utils.Replayer;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@SpringUI
@Push
@Profile("ui")
@RequiredArgsConstructor
@XSlf4j
public class WebUI extends UI {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final Snapshotter snapshotter;
    private final Replayer replayer;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        Panel commandPanel = commandPanel();
        Panel replayPanel = replayPanel();
        Panel queryPanel = queryPanel();
        Panel snapshotPanel = snapshot();

        HorizontalLayout panels12 = new HorizontalLayout();
        panels12.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        panels12.addComponents(commandPanel, queryPanel);
        panels12.setSizeFull();

        HorizontalLayout panels34 = new HorizontalLayout();
        panels34.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        panels34.addComponents(replayPanel, snapshotPanel);
        panels34.setSizeFull();

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.addComponents(panels12, panels34);

        setContent(verticalLayout);

        UI.getCurrent().setErrorHandler(new DefaultErrorHandler() {
            @Override
            public void error(com.vaadin.server.ErrorEvent event) {
                Throwable cause = event.getThrowable();
                log.error("an error occured", cause);
                while(cause.getCause() != null) cause = cause.getCause();
                Notification.show("Error", cause.getMessage(), Notification.Type.ERROR_MESSAGE);
            }
        });

    }

    private Panel commandPanel() {
        TextField id = new TextField("ID");
        TextField status = new TextField("Status");
        status.setValue(MockService.DEFAULT_STATUS);
        TextField issuetype = new TextField("Issue-Type");
        issuetype.setValue(MockService.DEFAULT_ISSUETYPE);
        TextField priority = new TextField("Priority");
        priority.setValue(MockService.DEFAULT_PRIORITY);
        TextField summary = new TextField("Summary");
        summary.setSizeFull();
        summary.setValue(MockService.DEFAULT_SUMMARY);

        Button add = new Button("AddArtifactCmd");

        add.addClickListener(evt -> {
            Artifact a = MockService.mockArtifact(id.getValue(), status.getValue(), issuetype.getValue(), priority.getValue(), summary.getValue());
            commandGateway.sendAndWait(new AddArtifactCmd(id.getValue(), a));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE);
        });

        FormLayout form = new FormLayout();
        form.addComponents(id, status, issuetype, priority, summary, add);
        form.setMargin(true);

        Panel panel = new Panel("Send commands");
        panel.setContent(form);
        return panel;
    }

    private Panel queryPanel() {
        TextField id = new TextField("ID");
        Button query = new Button("Query");

        query.addClickListener(evt -> {
            CompletableFuture<FindResponse> val = queryGateway.query(new FindQuery(id.getValue()), FindResponse.class);
            try {
                Notification.show(String.valueOf(val.get().getAmount()), Notification.Type.HUMANIZED_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        FormLayout form = new FormLayout();
        form.addComponents(id, query);
        form.setMargin(true);

        Panel panel = new Panel("Send history query");
        panel.setContent(form);
        return panel;
    }

    private Panel snapshot() {
        TextField snapshotTimestamp = new TextField("Snapshot Timestamp");
        snapshotTimestamp.setValue("2020-05-01T11:06:00.00Z");
        snapshotTimestamp.setWidth("210px");

        Button snapshot = new Button("Snapshot");

        snapshot.addClickListener(evt -> {
            snapshotter.replayEventsUntil(Instant.parse(snapshotTimestamp.getValue()));
        });

        FormLayout form = new FormLayout();
        form.addComponents(snapshotTimestamp, snapshot);
        form.setMargin(true);

        Panel panel = new Panel("Make Snapshot");
        panel.setContent(form);
        return panel;
    }

    private Panel replayPanel() {
        Button replay = new Button("Start Replay");
        replay.addClickListener(evt -> {
            replayer.replay("projection");
            Notification.show("Replaying..", Notification.Type.HUMANIZED_MESSAGE);
        });

        FormLayout form = new FormLayout();
        form.addComponents(replay);
        form.setMargin(true);

        Panel panel = new Panel("Replaying");
        panel.setContent(form);
        return panel;
    }
}
