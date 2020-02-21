package pingpong.query.history;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pingpong.api.*;
import pingpong.query.PingPongModel;
import pingpong.rulebase.RuleEvaluation;
import pingpong.utils.Replayer;

import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@XSlf4j
@RequiredArgsConstructor
@Component
@Profile("query")
@ProcessingGroup("history")
public class PingPongHistoryProjection {

    private final Replayer replayer;
    private final RuleEvaluation re = new RuleEvaluation();

    private PingPongModel model = PingPongModel.getInstance();
    private CLITool cli = new CLITool();

    @EventHandler
    public void on(CreatedEvt event, ReplayStatus replayStatus, @Timestamp Instant t) {
        log.debug("projecting (history) {}", event);
        model.createHistory(event.getId(), event.getAmount());
        checkForSnapshot(replayStatus, t);
    }

    @EventHandler
    public void on(IncreasedEvt event, ReplayStatus replayStatus, @Timestamp Instant t) {
        log.debug("projecting (history) {}", event);
        model.increaseHistory(event.getId(), event.getAmount());
        checkForSnapshot(replayStatus, t);
    }

    @EventHandler
    public void on(DecreasedEvt event, ReplayStatus replayStatus, @Timestamp Instant t) {
        log.debug("projecting (history) {}", event);
        model.decreaseHistory(event.getId(), event.getAmount());
        checkForSnapshot(replayStatus, t);
    }

    @QueryHandler
    public FindResponse handle(FindHistoryQuery query) {
        log.debug("handle (history) {}", query);
        model.print(); // TODO remove
        return null; // TODO
    }

    @ResetHandler
    public void reset() {
        log.debug("reset view db (history)");
        model.resetHistory();
    }

    private void checkForSnapshot(ReplayStatus replayStatus, Instant t) {
        if (replayStatus.isReplay()) {
            try {
                if (replayer.isAtSnapshotPosition(t)) {
                    log.debug("Replay is at snapshot position");
                    CompletableFuture<CLITool.Action> completableFuture = cli.readAction();
                    CLITool.Action action = completableFuture.get();
                    switch (action) {
                        case STORE:
                            model.createSnapshot();
                        case STEP:
                            replayer.setSnapshotTokenAt(t);
                            break;
                        default:
                            // do nothing
                    }
                }
            } catch (NullPointerException e) {
                log.error("Snapshot Token not valid!");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
