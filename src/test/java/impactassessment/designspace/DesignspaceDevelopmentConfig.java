package impactassessment.designspace;



import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.axonframework.commandhandling.gateway.CommandGateway;
import com.google.inject.Guice;
import com.google.inject.Injector;
import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import artifactapi.jira.IJiraArtifact;
import at.jku.designspace.sdk.clientservice.IDesignspaceChangeSubscriber;
import at.jku.designspace.sdk.clientservice.InstanceService;
import at.jku.designspace.sdk.clientservice.PolarionInstanceService;
import at.jku.designspace.sdk.clientservice.Service;
import at.jku.designspace.sdk.jira.JiraArtifact;
import at.jku.isse.designspace.sdk.core.DesignSpace;
import at.jku.isse.designspace.sdk.core.model.User;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.designspace.DesignspaceChangeSubscriber;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.artifactconnector.jira.JiraJsonService;
import impactassessment.command.MockCommandGateway;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;

public class DesignspaceDevelopmentConfig extends BaseConfig {

	protected Logger log = LogManager.getLogger(DesignspaceDevelopmentConfig.class);
	
	
	private WorkflowDefinitionRegistry registry;
	
	private CommandGateway gw;

	private ArtifactRegistry artReg;

	private PolarionInstanceService polarionService;
	private IJiraService jiraServiceWChangepoller;
	
	private static InstanceService<JiraArtifact> jiraService;
	
	public DesignspaceDevelopmentConfig() {
		artReg = new ArtifactRegistry();
		registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		gw = new MockCommandGateway(artReg, registry);
		User user = DesignSpace.registerUser("testuser");
		DesignspaceChangeSubscriber dcs = new DesignspaceChangeSubscriber(gw);
	    polarionService = new PolarionInstanceService(user, Service.POLARION, "ArtifactConnector"); //TODO: replace here with actual Project used for fetching workitems from
	    polarionService.addChangeSubscriber(dcs);
	    jiraServiceWChangepoller = getJiraDesignspaceService(dcs);
	    
	}
	
	protected void configure() {
		bind(CommandGateway.class).toInstance(gw);
		bind(IArtifactRegistry.class).toInstance(artReg);
		bind(PolarionInstanceService.class).toInstance(polarionService);
		bind(WorkflowDefinitionRegistry.class).toInstance(registry);
		bind(IJiraService.class).toInstance(jiraServiceWChangepoller);		
	}
	
	public IJiraService getJiraDesignspaceService(IDesignspaceChangeSubscriber dcs) {
        User user_ = DesignSpace.registerUser("felix");
        InstanceService<JiraArtifact> js_ = new InstanceService<JiraArtifact>(user_, Service.JIRA, JiraArtifact.class, IJiraArtifact.class);

        js_.addChangeSubscriber(dcs);
    	return new IJiraService() {
            User user = user_;
            InstanceService<JiraArtifact> js = js_;
			
    		public boolean provides(String type) {
				return js.provides(type);
			}
			public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
				return js.get(id, workflowId);
			}
			public void injectArtifactService(IArtifact artifact, String workFlowId) {
				js.injectArtifactService(artifact, workFlowId);
			}
			@Override
			public void deleteDataScope(String scopeId) {
				js.deleteDataScope(scopeId);
			}
			@Override
			public Optional<IJiraArtifact> getIssue(String id, String workflow) {
				return js.get(id, workflow).map(j -> j);
			}
			@Override
			public Optional<IJiraArtifact> getIssue(String key) {
				return js.get(key).map(j -> j);
			}			    		
    	} ;   	    
    }
	
	
	private static Injector inj;
	
	public static Injector getInjector() {
		if (inj == null)
			inj = Guice.createInjector(new DesignspaceDevelopmentConfig());
		return inj;
	}
	
	public static IJiraService getJiraDemoService() {
		if (jiraS == null)
				jiraS = new JiraJsonService();
		return jiraS;
	}
	
	public static InstanceService<JiraArtifact> getJiraService() { 
		if (jiraService == null) {
			User user = DesignSpace.registerUser("felix");		
			jiraService= new InstanceService<JiraArtifact>(user, Service.JIRA, JiraArtifact.class, IJiraArtifact.class);
		}
		return jiraService;
	}
	
	private static IJiraService jiraS;
	
    
}
