package impactassessment.registry;

import impactassessment.query.Replayer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public abstract class AbstractRegisterService implements IRegisterService {

    final WorkflowDefinitionRegistry registry;
    private Replayer replayer;

    public AbstractRegisterService(WorkflowDefinitionRegistry registry, Replayer replayer) {
        this.registry = registry;
        this.replayer = replayer;
    }

    /**
     * Source: https://www.baeldung.com/running-setup-logic-on-startup-in-spring
     *
     * "This approach can be used for running logic after the Spring context has been initialized,
     * so we are not focusing on any particular bean, but waiting for all of them to initialize."
     *
     * @param event "In this example we chose the ContextRefreshedEvent.
     *              Make sure to pick an appropriate event that suits your needs."
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        registerAll();
        if (replayer != null)
            replayer.replay("projection");
    }

}
