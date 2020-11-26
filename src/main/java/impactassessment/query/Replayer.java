package impactassessment.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Replayer {

    private final EventProcessingConfiguration configuration;

    public void replay(String name) {
        configuration.eventProcessorByProcessingGroup(name, TrackingEventProcessor.class)
                .ifPresent(trackingEventProcessor -> {
                    trackingEventProcessor.shutDown();
                    trackingEventProcessor.resetTokens();
                    trackingEventProcessor.start();
                });
    }
}
