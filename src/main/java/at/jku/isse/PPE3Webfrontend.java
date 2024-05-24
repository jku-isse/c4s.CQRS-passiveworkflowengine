package at.jku.isse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import at.jku.isse.designspace.core.events.Event;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.core.service.ServiceRegistry;
import at.jku.isse.designspace.core.service.WorkspaceService;

//@SpringBootApplication(exclude = DesignSpace.class)
//@EnableAutoConfiguration
//@ComponentScan(excludeFilters={@Filter(type=FilterType.CUSTOM, classes={at.jku.isse.PPE3Webfrontend.DesignSpaceTypeFilter.class})})
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class PPE3Webfrontend extends SpringBootServletInitializer implements ApplicationListener<ApplicationReadyEvent>  {

	private static ApplicationContext ctx;
	
    public static void main(String[] args) {
    	 ctx = SpringApplication.run(PPE3Webfrontend.class, args);

//        var user = User.users.get(1l);
//        user.clearNotifications();

        System.out.println("Successfully initialized!");
        System.out.println("======================================================================================================");
    }
    
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent  event) {
//    	ServiceRegistry.initializeAllPersistenceUnawareServices();
//        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();
//
//        Event.setInitialized();
//
//        ServiceRegistry.initializeAllPersistenceAwareServices();
//        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();
    }

}
