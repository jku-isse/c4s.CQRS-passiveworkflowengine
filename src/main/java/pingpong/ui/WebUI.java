package pingpong.ui;

import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryGateway;
import pingpong.api.*;
import com.vaadin.annotations.Push;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import pingpong.query.snapshot.Snapshotter;
import pingpong.utils.Replayer;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SpringUI
@Push
@Profile("ui")
@RequiredArgsConstructor
public class WebUI extends UI {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

        HorizontalLayout panels = new HorizontalLayout();
        panels.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        panels.addComponents(commandPanel, queryPanel, replayPanel, snapshotPanel);
        panels.setSizeFull();

        setContent(panels);

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
        TextField amount = new TextField("Amount");
        Button create = new Button("Create");
        Button increase = new Button("Increase");
        Button decrease = new Button("Decrease");

        create.addClickListener(evt -> {
            commandGateway.sendAndWait(new CreateCmd(id.getValue(), Integer.parseInt(amount.getValue())));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE);
        });

        increase.addClickListener(evt -> {
            commandGateway.sendAndWait(new IncreaseCmd(id.getValue(), Integer.parseInt(amount.getValue())));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE);
        });

        decrease.addClickListener(evt -> {
            commandGateway.sendAndWait(new DecreaseCmd(id.getValue(), Integer.parseInt(amount.getValue())));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE);
        });

        FormLayout form = new FormLayout();
        form.addComponents(id, amount, create, increase, decrease);
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
        snapshotTimestamp.setValue("2020-02-18T13:31:00.00Z");
        snapshotTimestamp.setWidth("300px");

        Button snapshot = new Button("Snapshot");

        snapshot.addClickListener(evt -> {
            snapshotter.replayEventsUntil(Instant.parse("2020-02-18T13:31:00.00Z"));
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
            replayer.replay("live");
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
