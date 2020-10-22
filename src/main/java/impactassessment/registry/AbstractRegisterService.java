package impactassessment.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public abstract class AbstractRegisterService implements IRegisterService {

    final WorkflowDefinitionRegistry registry;

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
    }

}
