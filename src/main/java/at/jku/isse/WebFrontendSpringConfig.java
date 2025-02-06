package at.jku.isse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.google.inject.Injector;

import at.jku.isse.artifacteventstreaming.rule.RuleSchemaProvider;
import at.jku.isse.designspace.artifactconnector.core.monitoring.IProgressObserver;
import at.jku.isse.designspace.artifactconnector.core.repository.IArtifactProvider;
import at.jku.isse.designspace.artifactconnector.core.security.EmailToIdMapper;
import at.jku.isse.designspace.azure.service.AzureServiceBuilder;
import at.jku.isse.passiveprocessengine.core.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.demo.DemoArtifactProvider;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.frontend.ProcessChangeListenerWrapper;
import at.jku.isse.passiveprocessengine.frontend.ProcessChangeNotifier;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;
import at.jku.isse.passiveprocessengine.frontend.oclx.OCLXSupportSetup;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.monitoring.ProgressPusher;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.providers.ProcessConfigProvider;
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class WebFrontendSpringConfig {
	
	/* Remaining Frontend Setup beyond what is done in RestFrontend Project*/	 	

	@Bean @Primary // overriding basic resolver from RestFrontend
	public ArtifactResolver getArtifactResolverForWebfrontend(AzureServiceBuilder azureBuilder,
			 ProcessConfigProvider procconf, ProcessRegistry procReg
			 , UIConfig uiConfig, InstanceRepository repo, SchemaRegistry schemaReg) {
		IArtifactProvider azure = azureBuilder.build();
		
		ArtifactResolver ar = new ArtifactResolver(repo);
		ar.register(azure);
		ar.register(procconf);
		
		// we dont want to polute schema with demo artifact if demo mode is off
		if (uiConfig.isDemoModeEnabled()) {
			var demoArtFactory = new TestArtifacts(repo, schemaReg);	
			PPEInstance jiraB =  demoArtFactory.getJiraInstance("IssueB");
    		PPEInstance jiraC = demoArtFactory.getJiraInstance("IssueC");		
    		PPEInstance jiraA = demoArtFactory.getJiraInstance("IssueA", jiraB, jiraC);
			var demoissueProvider = new DemoArtifactProvider(schemaReg, repo, demoArtFactory);
			ar.register(demoissueProvider);
		}
		
		return ar;
	}	  

    @Bean @Primary // overriding basic listener from RestFrontend
    public static ProcessChangeListenerWrapper getProcessChangeListenerWrapperForWebfrontend(ChangeEventTransformer changeEventTransformer, ProcessContext ctx, ArtifactResolver resolver
    		, EventDistributor eventDistributor, IFrontendPusher uiUpdater, RuleSchemaProvider ruleSchemaProvider) {
    	ProcessChangeNotifier picp = new ProcessChangeNotifier(ctx, uiUpdater, resolver, eventDistributor, ruleSchemaProvider );
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
	public static EmailToIdMapper getNoOpEmailToIdMapper() {
		return new EmailToIdMapper() {
			@Override
			public String getIdForEmail(String email) {
				return "NONE";
			}			
		};		
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

	private static  Injector injector;
	
	@Bean @Primary
	public static Injector getOCLXDependencies(SchemaRegistry designspace) {
		if (injector == null) {
			var	setup = new OCLXSupportSetup(designspace);
			injector = setup.createInjectorAndDoEMFRegistration();
		}
		return injector;
	}

	private static CodeActionExecuterProvider codeActionExecuterProvider;
	
	@Bean
	public static CodeActionExecuterProvider getCodeActionExecuterProvider(Injector injector) {
		if (codeActionExecuterProvider == null) {
			codeActionExecuterProvider = new CodeActionExecuterProvider();
			injector.injectMembers(codeActionExecuterProvider);
		}
		return codeActionExecuterProvider;
	}

}
