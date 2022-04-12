package at.jku.isse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import at.jku.isse.designspace.core.events.Event;
import at.jku.isse.designspace.core.model.PublicWorkspace;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.endpoints.grpc.service.GrpcUtils;

//@SpringBootApplication(exclude = DesignSpace.class)
//@EnableAutoConfiguration
//@ComponentScan(excludeFilters={@Filter(type=FilterType.CUSTOM, classes={at.jku.isse.PPE3Webfrontend.DesignSpaceTypeFilter.class})})
@SpringBootApplication
public class PPE3Webfrontend extends SpringBootServletInitializer implements ApplicationListener<ApplicationStartedEvent>  {

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
    public void onApplicationEvent(final ApplicationStartedEvent event) {
    	if (!Event.isInitialized())
    		Event.setInitialized();
    }

    @PreDestroy
    public void onExit() {
        GrpcUtils.closeCapture();
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
