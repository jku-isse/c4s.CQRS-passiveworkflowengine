package at.jku.isse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import at.jku.isse.designspace.artifactconnector.core.monitoring.IProgressObserver;
import at.jku.isse.designspace.artifactconnector.core.repository.IArtifactProvider;
import at.jku.isse.designspace.azure.service.AzureServiceBuilder;
import at.jku.isse.designspace.rule.arl.repair.analyzer.RepairAnalyzerForRestrictionAnalysis;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.passiveprocessengine.core.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.designspace.RuleServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.ProcessChangeListenerWrapper;
import at.jku.isse.passiveprocessengine.frontend.ProcessChangeNotifier;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.monitoring.ProgressPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.providers.ProcessConfigProvider;
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class WebFrontendSpringConfig {
	
	/* Remaining Frontend Setup beyond what is done in RestFrontend Project*/	 	

	@Bean @Primary // overriding basic resolver from RestFrontend
	public ArtifactResolver getArtifactResolverForWebfrontend(AzureServiceBuilder azureBuilder,
			/* IGitService github,   IJiraService jira, IJamaService jama, */ ProcessConfigProvider procconf, ProcessRegistry procReg ) {
		IArtifactProvider azure = azureBuilder.build();
		
		ArtifactResolver ar = new ArtifactResolver();
		ar.register(azure);
		/* ar.register(github); 
		ar.register(jira);
		ar.register(jama);*/
		ar.register(procconf);
		return ar;
	}	  

    @Bean @Primary
    public static ProcessChangeListenerWrapper getProcessChangeListenerWrapperForWebfrontend(ChangeEventTransformer changeEventTransformer, ProcessContext ctx, ArtifactResolver resolver, EventDistributor eventDistributor, IFrontendPusher uiUpdater, UsageMonitor usageMonitor) {
    	ProcessChangeNotifier picp = new ProcessChangeNotifier(ctx, uiUpdater, resolver, eventDistributor, usageMonitor );
		changeEventTransformer.registerWithWorkspace(picp);
		return picp;
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
    
	@Bean @Primary
	public static IProgressObserver getProgressPusher(ITimeStampProvider timeProvider) {
		return new ProgressPusher(timeProvider);
	}
	
	@Bean 
	public static RepairAnalyzerForRestrictionAnalysis getRepairAnalyzerForRestrictionAnalysis(RuleServiceWrapper monitor, ArlRuleEvaluator are) {
		RepairAnalyzerForRestrictionAnalysis restrAnalyzer = new RepairAnalyzerForRestrictionAnalysis(monitor); 
		are.registerListener(restrAnalyzer);		
		return restrAnalyzer;
	}
	
//    @Bean
//    public RepairAnalyzer getRepairAnalyzer(RepairStats rs, ITimeStampProvider tsProvider, UsageMonitor monitor) {
//    	RepairNodeScorer scorer= new NoSort();
//    	RepairFeatureToggle rtf=new RepairFeatureToggle(true,false,false);
//    	return new RepairAnalyzer(null,rs, scorer, tsProvider, monitor,rtf); // workspace will/must be injected in RequestDelegate    	
//    	
//		if (repAnalyzer != null)
//			repAnalyzer.inject(ws);
//		ArlRuleEvaluator arl = new ArlRuleEvaluator();
//		arl.registerListener(repAnalyzer);
//		RuleService.setEvaluator(arl);
//		//RuleService.currentWorkspace = ws;
//    }




}
