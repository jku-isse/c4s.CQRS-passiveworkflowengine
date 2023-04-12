package at.jku.isse;

import java.io.FileReader;
import java.io.IOException;

//import javax.jms.ConnectionFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import at.jku.isse.designspace.azure.service.IAzureService;
import at.jku.isse.designspace.git.service.IGitService;
import at.jku.isse.designspace.jama.service.IJamaService;
import at.jku.isse.designspace.jira.service.IJiraService;
import at.jku.isse.designspace.rule.arl.repair.order.RepairNodeScorer;
import at.jku.isse.designspace.rule.arl.repair.order.RepairStats;
import at.jku.isse.designspace.rule.arl.repair.order.SortOnRepairPercentage;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.artifacts.DemoServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.registry.TriggeredProcessLoader;
import at.jku.isse.passiveprocessengine.frontend.ui.monitoring.ProgressPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStateChangeLog;
import at.jku.isse.passiveprocessengine.monitoring.RepairAnalyzer;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.extern.slf4j.Slf4j;



@Configuration
@Slf4j
public class FrontendSpringConfig {


//	private UpdateMemory updateMemory;


//	private UpdateManager updateManager;

//	@Bean
//	@DependsOn({"controlEventEngine"})
//	public UpdateMemory initUpdateMemory() {
//		this.updateMemory = new UpdateMemory();
//		return this.updateMemory;
//	}

//	@Bean
//	@DependsOn({"controlEventEngine"})
//	public UpdateManager initUpdateManager() {
//		this.updateManager = new UpdateManager();
//		updateManager.init();
//		return this.updateManager;
//	}

	//------------------------------------------------------------------------------------------------------------------
	//-------------------------------------------PROJECT COMPONENTS-----------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------

	
	@Bean UIConfig getUIConfig() {
		UIConfig props = new UIConfig();
		try {
            FileReader reader = new FileReader("./application.properties");
            props.load(reader);
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
//	@DependsOn({"controlEventEngine"})
	ProcessRegistry getProcessRegistry() {
		return new ProcessRegistry();
	}

	@Bean
//	@DependsOn({"controlEventEngine"})
	public TriggeredProcessLoader getProcessLoader(ProcessRegistry registry) {
		return new TriggeredProcessLoader(registry);
	}

	@Bean
//	@DependsOn({"controlEventEngine"})
	public ArtifactResolver getArtifactResolver(IAzureService azure, IGitService github, DemoServiceWrapper demo, IJiraService jira, IJamaService jama, ProcessRegistry procReg ) {
		ArtifactResolver ar = new ArtifactResolver();
		ar.register(azure);
		ar.register(github);
		ar.register(demo);
		ar.register(jira);
		ar.register(jama);
		return ar;
	}
    
    @Bean
    public RepairStats getRepairStats() {
    	return new RepairStats();
    }
	
    @Bean
    public RepairAnalyzer getRepairAnalyzer(RepairStats rs, ITimeStampProvider tsProvider) {
    	RepairNodeScorer scorer= new SortOnRepairPercentage();
    	return new RepairAnalyzer(null,rs, scorer, tsProvider); // workspace will/must be injected in RequestDelegate    	
    }

	
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
