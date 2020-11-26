package impactassessment.jiraartifact;

import c4s.jiralightconnector.ChangeStreamPoller;
import impactassessment.ui.FrontendPusher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraPoller implements Runnable {

    private final ChangeStreamPoller changeStreamPoller;
    private final FrontendPusher pusher;

    private Thread worker;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);

    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    public void interrupt() {
        running.set(false);
        worker.interrupt();
    }

    boolean isRunning() {
        return running.get();
    }

    boolean isStopped() {
        return stopped.get();
    }

    public void setInterval(int minutes) {
        changeStreamPoller.setInterval(minutes);
    }

    @Override
    public void run() {
        running.set(true);
        stopped.set(false);
        while (running.get()) {
            try {
                changeStreamPoller.run();
            } catch (NullPointerException e) {
                log.error("Catches NullPointerException due to: SSLPeerUnverifiedException");
            }
            try {
                log.info("go to sleep");
                Thread.sleep(changeStreamPoller.getIntervalInMinutes() * 60 * 1000);
                log.info("awake");
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
            if (running.get()) {
                pusher.updateFetchTimer();
            }
        }
        log.info("stopped");
        stopped.set(true);
    }

}
