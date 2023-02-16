package at.jku.isse.passiveprocessengine.frontend.registry;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import at.jku.isse.passiveprocessengine.definition.serialization.FilesystemProcessDefinitionLoader;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;

@Component
public class TriggeredProcessLoader extends FilesystemProcessDefinitionLoader{
       
	public TriggeredProcessLoader(ProcessRegistry registry) {
		super(registry);
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
    public void onApplicationEvent(ApplicationReadyEvent event) { //ContextRefreshedEvent event) {
        registerAll();
    }

}
