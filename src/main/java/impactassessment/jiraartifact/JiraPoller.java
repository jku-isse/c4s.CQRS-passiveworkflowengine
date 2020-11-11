package impactassessment.jiraartifact;

import c4s.jiralightconnector.ChangeStreamPoller;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraPoller implements Runnable {

    private final ChangeStreamPoller changeStreamPoller;
    private volatile boolean enabled = true;

    public void setInterval(int minutes) {
        changeStreamPoller.setInterval(minutes);
    }

    public void stop() {
        enabled = false;
    }

    @SneakyThrows
    @Override
    public void run() {
        enabled = true;
        while(enabled) {
            log.info("Fetch updates");
            for (int i = 0; i < 3; i++) { // FIXME: dirty solution because first access throws: SSLPeerUnverifiedException
                Thread thread = new Thread(changeStreamPoller);
                thread.start();
                thread.join();
            }
            Thread.sleep(changeStreamPoller.getIntervalInMinutes() * 60 * 1000);
        }
        log.info("Worker stopped");
    }


}
