package at.jku.isse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import at.jku.isse.designspace.azure.service.IAzureService;
//import at.jku.isse.designspace.git.service.IGitService;
import at.jku.isse.designspace.jama.service.IJamaService;
import at.jku.isse.designspace.jira.service.IJiraService;
import at.jku.isse.designspace.rule.arl.repair.order.NoSort;
import at.jku.isse.designspace.rule.arl.repair.order.RepairNodeScorer;
import at.jku.isse.designspace.rule.arl.repair.order.RepairStats;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.core.ConfigurationBuilder;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleDefinitionFactory;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.designspace.DesignspaceAbstractionMapper;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.designspace.RuleServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ProcessConfigProvider;
import at.jku.isse.passiveprocessengine.frontend.registry.TriggeredProcessLoader;
import at.jku.isse.passiveprocessengine.frontend.ui.monitoring.ProgressPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStateChangeLog;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FrontendSpringConfig {

	
	/* Designspace Setup*/
	// is done in wrapper project
	
	/* Basic Process Engine Setup*/
	
	@Bean
	public RepairTreeProvider getRepairTreeProvider(DesignspaceAbstractionMapper designspaceAbstractionMapper) {
		return new RuleServiceWrapper(designspaceAbstractionMapper);
	}
	
	@Bean
	public ConfigurationBuilder configBuilder(SchemaRegistry schemaReg, InstanceRepository instanceRepository, RepairTreeProvider repairTreeProvider, DesignspaceAbstractionMapper designspaceAbstractionMapper, RuleDefinitionFactory ruleDefinitionFactory) {
		return new ConfigurationBuilder(schemaReg, instanceRepository, repairTreeProvider, new RewriterFactory(designspaceAbstractionMapper), ruleDefinitionFactory);
	}
	
	@Bean
	public ProcessContext getProcessContext(ConfigurationBuilder configBuilder) {
		return configBuilder.getContext();
	}
	
	/* Frontend Setup*/
	
	@Bean 
	ProcessRegistry getProcessRegistry(ProcessContext context) {
		return new ProcessRegistry(context);
	}

	@Bean
	public TriggeredProcessLoader getProcessLoader(ProcessRegistry registry) {
		return new TriggeredProcessLoader(registry);
	}
		

	@Bean
	public ArtifactResolver getArtifactResolver(IAzureService azure,
			/* IGitService github, */  IJiraService jira, IJamaService jama, ProcessConfigProvider procconf, ProcessRegistry procReg ) {
		ArtifactResolver ar = new ArtifactResolver();
		ar.register(azure);
		/* ar.register(github); */
		ar.register(jira);
		ar.register(jama);
		ar.register(procconf);
		return ar;
	}
	
    @Bean 
    public ITimeStampProvider getTimeStampProvider() {
    	return new CurrentSystemTimeProvider();
    }
    
	@Bean UIConfig getUIConfig(ApplicationContext context) {
		String  version = PPE3Webfrontend.class.getPackage().getImplementationVersion();
		UIConfig props = new UIConfig(version);
		
		try {
			InputStream input = new FileInputStream("application.properties") ;
            props.load(input);
		} catch(IOException e) {
			String msg = "No ./application.properties found";
			log.error(msg);
			throw new RuntimeException(msg);
		}
		return props;
	}

	@Bean 
	public ProgressPusher getIProgressObserver(ITimeStampProvider tsProvider) {
		return new ProgressPusher(tsProvider);
	}
    
    @Bean 
    public UsageMonitor getUsageMonitor(ITimeStampProvider timeprovider, RepairTreeProvider repairTreeProvider) {
    	return new UsageMonitor(timeprovider, repairTreeProvider);
    }
    
    @Bean
    public EventDistributor getEventDistributor(ITimeStampProvider timeprovider) {
    	EventDistributor ed = new EventDistributor();
    	ed.registerHandler(new ProcessMonitor(timeprovider));
    	return ed;
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
    public RepairStats getRepairStats() {
    	return new RepairStats();
    }
    
    @Bean
    public RepairAnalyzer getRepairAnalyzer(RepairStats rs, ITimeStampProvider tsProvider, UsageMonitor monitor) {
    	RepairNodeScorer scorer= new NoSort();
    	RepairFeatureToggle rtf=new RepairFeatureToggle(true,false,false);
    	return new RepairAnalyzer(null,rs, scorer, tsProvider, monitor,rtf); // workspace will/must be injected in RequestDelegate    	
    	
		if (repAnalyzer != null)
			repAnalyzer.inject(ws);
		ArlRuleEvaluator arl = new ArlRuleEvaluator();
		arl.registerListener(repAnalyzer);
		RuleService.setEvaluator(arl);
		//RuleService.currentWorkspace = ws;
    }

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}



}
