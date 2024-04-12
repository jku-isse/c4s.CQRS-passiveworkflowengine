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
//        if (args.length>0) {
//            if (args[0].equals("-capture"))
//                GrpcUtils.captureFileName=args[1];
//            else if (args[0].equals("-replay"))
//                GrpcUtils.replayFileName=args[1];
//        }
    	 ctx = SpringApplication.run(PPE3Webfrontend.class, args);
       // SpringApplication application = new SpringApplication(PPE3Webfrontend.class);
       // application.setBanner(new CustomBanner());
       // application.run(args);


        var user = User.users.get(1l);
        user.clearNotifications();

        System.out.println("Successfully initialized!");
        System.out.println("======================================================================================================");
    }
    
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent  event) {
    	ServiceRegistry.initializeAllPersistenceUnawareServices();
        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();

        Event.setInitialized();

        ServiceRegistry.initializeAllPersistenceAwareServices();
        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();
    }


	
//    public static class DesignSpaceTypeFilter implements TypeFilter {
//
//		@Override
//		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
//				throws IOException {
//			ClassMetadata classMetadata = metadataReader.getClassMetadata();
//	        String fullyQualifiedName = classMetadata.getClassName();
//	        String className = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf(".") + 1);
//	        return className.equalsIgnoreCase(DesignSpace.class.getSimpleName());
//		}
//    	
//    }
}
