package at.jku.isse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import at.jku.isse.designspace.azure.service.IAzureService;
import at.jku.isse.designspace.core.events.Event;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.ServiceRegistry;
import at.jku.isse.designspace.core.service.WorkspaceService;
//import at.jku.isse.designspace.git.service.IGitService;
import at.jku.isse.designspace.jama.service.IJamaService;
import at.jku.isse.designspace.jira.service.IJiraService;
import at.jku.isse.designspace.rule.arl.repair.analyzer.RepairAnalyzer;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.artifacts.DemoServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ProcessConfigProvider;
import at.jku.isse.passiveprocessengine.frontend.registry.TriggeredProcessLoader;
import at.jku.isse.passiveprocessengine.frontend.ui.monitoring.ProgressPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ExecutedRepiarListenerImp;
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStateChangeLog;
import at.jku.isse.passiveprocessengine.monitoring.RepairFeatureToggle;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FrontendSpringConfig {

public static AtomicBoolean isInit = new AtomicBoolean(false);
	
	private static void init() {
		if (isInit.compareAndExchange(false, true) == false) {
			try {
				new WorkspaceService();
				new RuleService();
			} catch (IOException e) {				
				e.printStackTrace();
				throw new RuntimeException("Error starting Workspace Service");
			} // to force PUBLIC WORKSPACE IS AVAILABLE

			ServiceRegistry.initializeAllPersistenceUnawareServices();
	        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();

	        Event.setInitialized();

	        ServiceRegistry.initializeAllPersistenceAwareServices();
	        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();  
		}
	}
	
	@Bean UIConfig getUIConfig(ApplicationContext context) {
		
		
		String  version = PPE3Webfrontend.class.getPackage().getImplementationVersion();
		UIConfig props = new UIConfig(version);
		
		try {
			InputStream input = new FileInputStream("application.properties") ;
            props.load(input);
		} catch(IOException e) {
			log.error("No ./application.properties found");
		}
		return props;
	}

	@Bean 
	public ProgressPusher getIProgressObserver(ITimeStampProvider tsProvider) {
		return new ProgressPusher(tsProvider);
	}
	
	@Bean 
	ProcessRegistry getProcessRegistry() {
		return new ProcessRegistry();
	}

	@Bean
	public TriggeredProcessLoader getProcessLoader(ProcessRegistry registry) {
		return new TriggeredProcessLoader(registry);
	}
		
	@Bean Workspace getWorkspace() {
		init();
		return WorkspaceService.PUBLIC_WORKSPACE;
	}
	
	@Bean ProcessConfigBaseElementFactory getProcessConfigBaseElementFactory(Workspace workspace) {
		return new ProcessConfigBaseElementFactory(workspace);
	}
	
	@Bean ProcessConfigProvider getProcessConfigProvider(ProcessConfigBaseElementFactory configFactory, Workspace workspace) {
		return new ProcessConfigProvider(configFactory, workspace);
	}

	@Bean
	public ArtifactResolver getArtifactResolver(IAzureService azure,
			/* IGitService github, */ DemoServiceWrapper demo, IJiraService jira, IJamaService jama, ProcessConfigProvider procconf, ProcessRegistry procReg ) {
		ArtifactResolver ar = new ArtifactResolver();
		ar.register(azure);
		/* ar.register(github); */
		ar.register(demo);
		ar.register(jira);
		ar.register(jama);
		ar.register(procconf);
		return ar;
	}
    
//    @Bean
//    public RepairStats getRepairStats() {
//    	return new RepairStats();
//    }
//	
	
	@Bean
	public RepairAnalyzer getRepairAnalyzer(ITimeStampProvider tsProvider, UsageMonitor monitor) {
    	return new at.jku.isse.designspace.rule.arl.repair.analyzer.RepairAnalyzer(null,new ExecutedRepiarListenerImp(new UsageMonitor(tsProvider))); 
	}
	
//    @Bean
//    public RepairAnalyzer getRepairAnalyzer(RepairStats rs, ITimeStampProvider tsProvider, UsageMonitor monitor) {    	 
//    	RepairNodeScorer scorer= new NoSort();
//    	RepairFeatureToggle rtf=new RepairFeatureToggle(true,false,false);
//    	return new RepairAnalyzer(null,rs, scorer, tsProvider, monitor,rtf); // workspace will/must be injected in RequestDelegate    	
//    }

	
    @Bean 
    public ITimeStampProvider getTimeStampProvider() {
    	return new CurrentSystemTimeProvider();
    }
    
    @Bean 
    public UsageMonitor getUsageMonitor(ITimeStampProvider timeprovider) {
    	return new UsageMonitor(timeprovider);
    }
    
    @Bean 
    public ProcessStateChangeLog getProcessStateChangeLog(EventDistributor ed) {
    	ProcessStateChangeLog logs = new ProcessStateChangeLog();
    	ed.registerHandler(logs);
    	return logs;
    }
    
    @Bean
    public ProcessQAStatsMonitor getProcessQAStatsMonitor(EventDistributor ed) {
    	ProcessQAStatsMonitor qaMonitor = new ProcessQAStatsMonitor();
    	ed.registerHandler(qaMonitor);
    	return qaMonitor;
    }
    
    @Bean
    public EventDistributor getEventDistributor(ITimeStampProvider timeprovider) {
    	EventDistributor ed = new EventDistributor();
    	ed.registerHandler(new ProcessMonitor(timeprovider));
    	return ed;
    }

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}



}
