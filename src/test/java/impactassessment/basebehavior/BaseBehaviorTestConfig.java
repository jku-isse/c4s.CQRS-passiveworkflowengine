package impactassessment.basebehavior;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.core.io.ClassPathResource;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import artifactapi.IArtifactRegistry;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.demo.DemoService;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.EventList2Logger;
import impactassessment.query.NoOpHistoryLogEventLogger;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

public class BaseBehaviorTestConfig extends AbstractModule {

	protected Logger log = LogManager.getLogger(BaseBehaviorTestConfig.class);
	
	
	private WorkflowDefinitionRegistry registry;
	
	private CommandGateway gw;

	private ArtifactRegistry artReg;
	
	private DemoService ds;
	
	WorkflowProjection wfp;
	ProjectionModel pModel;
	IKieSessionService kieS;

	
	public BaseBehaviorTestConfig() {
		artReg = new ArtifactRegistry();
		registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		gw = new MockCommandGateway(artReg, registry);
		ds = new DemoService();
		artReg.register(ds);
		pModel = new ProjectionModel(artReg);
		IFrontendPusher fp = new SimpleFrontendPusher();
		kieS = new SimpleKieSessionService(gw, artReg);
		wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, artReg, new EventList2Logger(new NoOpHistoryLogEventLogger()));
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
	}
	
	protected void configure() {
		bind(CommandGateway.class).toInstance(gw);
		bind(IArtifactRegistry.class).toInstance(artReg);
		bind(WorkflowDefinitionRegistry.class).toInstance(registry);
		bind(DemoService.class).toInstance(ds);
		bind(WorkflowProjection.class).toInstance(wfp);
		bind(IKieSessionService.class).toInstance(kieS);
		bind(ProjectionModel.class).toInstance(pModel);
	}
	
	private static Injector inj;
	
	public static Injector getInjector() {
		if (inj == null)
			inj = Guice.createInjector(new BaseBehaviorTestConfig());
		return inj;
	}
	

	
	
    private Properties props = null;
    
    private String getProp(String name) {
        return getProp(name, null);
    }

    private String getProp(String name, String defaultValue) {
        if (props == null) {
            props = getProps();
        }
        String value = props.getProperty(name, defaultValue);
        if (value == null) {
        	log.error("Required property "+name+" was not found in the application properties!");
        }
        return value;
    }
    
    private Properties getProps() {
        Properties props = new Properties();
        // try to use external first
        try {
            FileReader reader = new FileReader(new File("./main.properties"));
            props.load(reader);
            return props;
        } catch (IOException e1) {
        	log.info("No properties file in default location (same directory as JAR) found! Using default props.");
            try {
                InputStream inputStream = new ClassPathResource("application.properties").getInputStream();
                props.load(inputStream);
                return props;
            } catch (IOException e2) {
                log.error("No properties file found.");
                e2.printStackTrace();
            }
        }
        return props;
    }
}
