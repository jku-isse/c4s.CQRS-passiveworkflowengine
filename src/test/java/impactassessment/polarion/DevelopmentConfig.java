package impactassessment.polarion;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.hibernate.SessionFactory;
import org.springframework.core.io.ClassPathResource;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.httpconnection.ApacheHttpClient;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;

import artifactapi.IArtifactRegistry;
import at.jku.designspace.sdk.clientservice.PolarionInstanceService;
import at.jku.designspace.sdk.clientservice.Service;
import at.jku.isse.designspace.sdk.core.DesignSpace;
import at.jku.isse.designspace.sdk.core.model.User;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.analytics.JamaUpdateTracingInstrumentation;
import c4s.jamaconnector.cache.CacheStatus;
import c4s.jamaconnector.cache.CachedResourcePool;
import c4s.jamaconnector.cache.CachingJsonHandler;
import c4s.jamaconnector.cache.hibernate.HibernateBackedCache;
import c4s.jamaconnector.cache.hibernate.HibernateCacheStatus;
import c4s.jiralightconnector.ChangeSubscriber;
import c4s.jiralightconnector.IssueCache;
import c4s.jiralightconnector.JiraInstance;
import c4s.jiralightconnector.MonitoringState;
import c4s.jiralightconnector.analytics.JiraUpdateTracingInstrumentation;
import c4s.jiralightconnector.anonymizer.AnonymizingAsyncJiraRestClientFactory;
import c4s.jiralightconnector.anonymizer.AnonymizingJiraInstance;
import c4s.jiralightconnector.hibernate.HibernateBackedMonitoringState;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.artifactconnector.jira.JiraJsonService;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.command.MockCommandGateway;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;

public class DevelopmentConfig extends AbstractModule {

	protected Logger log = LogManager.getLogger(DevelopmentConfig.class);
	
	
	private WorkflowDefinitionRegistry registry;
	
	private CommandGateway gw;

	private ArtifactRegistry artReg;

	private PolarionInstanceService polarionService;
	
	public DevelopmentConfig() {
		artReg = new ArtifactRegistry();
		registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		gw = new MockCommandGateway(artReg, registry);
		User user = DesignSpace.registerUser("felix");
	    polarionService = new PolarionInstanceService(user, Service.POLARION, "ArtifactConnector");
	}
	
	protected void configure() {
		bind(CommandGateway.class).toInstance(gw);
		bind(IArtifactRegistry.class).toInstance(artReg);
		bind(PolarionInstanceService.class).toInstance(polarionService);
		bind(WorkflowDefinitionRegistry.class).toInstance(registry);
	}
	
	
	
	private static Injector inj;
	
	public static Injector getInjector() {
		if (inj == null)
			inj = Guice.createInjector(new DevelopmentConfig());
		return inj;
	}
	
	public static IJiraService getJiraDemoService() {
		if (jiraS == null)
				jiraS = new JiraJsonService();
		return jiraS;
	}
	
	private static IJiraService jiraS;
	
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
