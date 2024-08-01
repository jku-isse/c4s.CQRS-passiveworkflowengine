package at.jku.isse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

//@SpringBootApplication(exclude = DesignSpace.class)
//@EnableAutoConfiguration
//@ComponentScan(excludeFilters={@Filter(type=FilterType.CUSTOM, classes={at.jku.isse.PPE3Webfrontend.DesignSpaceTypeFilter.class})})
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class PPE3Webfrontend extends SpringBootServletInitializer implements ApplicationListener<ApplicationReadyEvent>  {

	private static ApplicationContext ctx;
	
    public static void main(String[] args) {
    	ctx = SpringApplication.run(PPE3Webfrontend.class, args);
        System.out.println("Successfully initialized!");
        System.out.println("======================================================================================================");
    }
    
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent  event) {
    	// DONE BY BaseConfiguration in DS Wrapper
    }

}
