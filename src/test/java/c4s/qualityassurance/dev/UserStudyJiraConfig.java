package c4s.qualityassurance.dev;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.hibernate.SessionFactory;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.httpconnection.ApacheHttpClient;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;

import artifactapi.IArtifactRegistry;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.analytics.JamaUpdateTracingInstrumentation;
import c4s.jamaconnector.cache.CacheStatus;
import c4s.jamaconnector.cache.CachedResourcePool;
import c4s.jamaconnector.cache.CachingJsonHandler;
import c4s.jamaconnector.cache.hibernate.HibernateBackedCache;
import c4s.jamaconnector.cache.hibernate.HibernateCacheStatus;
import c4s.jiralightconnector.ChangeSubscriber;
import c4s.jiralightconnector.InMemoryMonitoringState;
import c4s.jiralightconnector.IssueAgent;
import c4s.jiralightconnector.IssueCache;
import c4s.jiralightconnector.JiraInstance;
import c4s.jiralightconnector.MonitoringState;
import c4s.jiralightconnector.analytics.JiraUpdateTracingInstrumentation;
import c4s.jiralightconnector.anonymizer.AnonymizingJiraInstance;
import c4s.jiralightconnector.hibernate.HibernateBackedMonitoringState;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.artifactconnector.jira.JiraJsonService;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.artifactconnector.jira.MockedJiraCache;
import impactassessment.artifactconnector.usage.InMemoryPerProcessArtifactUsagePersistor;
import impactassessment.command.CollectingGatewayProxyFactory;
import impactassessment.command.IGatewayProxyFactory;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.Event2JsonProcessor;
import impactassessment.query.EventList2Forwarder;
import impactassessment.query.NoOpHistoryLogEventLogger;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

import org.springframework.core.io.ClassPathResource;


public class UserStudyJiraConfig extends AbstractModule {

    protected Logger log = LogManager.getLogger(UserStudyJiraConfig.class);

          
    private JiraRestClient jrc;
    private IssueCache jiraCache;
    private JiraUpdateTracingInstrumentation juti;
    private JiraChangeSubscriber jiraCS;
    private MonitoringState jiraM;
    
    private ArtifactRegistry artReg;
    private WorkflowDefinitionRegistry registry;
    private CommandGateway gw;
    WorkflowProjection wfp;
   	ProjectionModel pModel;
   	IKieSessionService kieS;
   	EventList2Forwarder el2f = new EventList2Forwarder();

    public UserStudyJiraConfig() {
    	
    	artReg = new ArtifactRegistry();
        registry = new WorkflowDefinitionRegistry();
        LocalRegisterService lrs = new LocalRegisterService(registry);
        lrs.registerAll();
        gw = new MockCommandGateway(artReg, registry);
    	
    	
        jrc = setupJiraRestClient();
        jiraCache = new MockedJiraCache();
        juti = setupJiraUpdateTracingInstrumentation();
        jiraM = new InMemoryMonitoringState();
        jiraCS = new JiraChangeSubscriber(null, new NoOpPerprocessArtifactUsagePersistor());
        
        pModel = new ProjectionModel(artReg);
        IGatewayProxyFactory gpf = new CollectingGatewayProxyFactory(gw);
        kieS = new SimpleKieSessionService(artReg, gpf);
        IFrontendPusher fp = new SimpleFrontendPusher();
        el2f.registerProcessor( new Event2JsonProcessor(new NoOpHistoryLogEventLogger()));
        wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, artReg, el2f);
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
    }

    protected void configure() {
    	bind(CommandGateway.class).toInstance(gw);
        bind(IArtifactRegistry.class).toInstance(artReg);
        bind(WorkflowDefinitionRegistry.class).toInstance(registry);
		bind(WorkflowProjection.class).toInstance(wfp);
		bind(IKieSessionService.class).toInstance(kieS);
		bind(ProjectionModel.class).toInstance(pModel);
		
    	bind(JiraRestClient.class).toInstance(jrc);
        bind(JiraUpdateTracingInstrumentation.class).toInstance(juti);        
        bind(JiraChangeSubscriber.class).toInstance(jiraCS);
        bind(ChangeSubscriber.class).toInstance(jiraCS);
        bind(MonitoringState.class).toInstance(jiraM);
        bind(IssueCache.class).toInstance(jiraCache);
        bind(AnonymizingJiraInstance.class).asEagerSingleton();
    }

    private static JiraService configJiraService(JiraInstance jiraI, JiraChangeSubscriber jiraCS) {
        return new JiraService(jiraI, jiraCS);
    }


    private static Injector inj;
    private static IJiraService jiraS;

    public static Injector getInjector() {
        if (inj == null)
            inj = Guice.createInjector(new UserStudyJiraConfig());
        return inj;
    }

    public static IJiraService getJiraService(boolean doCreateDemo) {
        if (jiraS == null)
            if (doCreateDemo) {
                jiraS = new JiraJsonService();
            } else
                jiraS = configJiraService(getInjector().getInstance(JiraInstance.class), getInjector().getInstance(JiraChangeSubscriber.class));
        return jiraS;
    }



    private JiraRestClient setupJiraRestClient() {
        String uri =  getProp("jiraServerURI");
        String username =  getProp("jiraConnectorUsername");
        String pw =  getProp("jiraConnectorPassword");
        return new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(URI.create(uri), username, pw);
    }

    private JiraUpdateTracingInstrumentation setupJiraUpdateTracingInstrumentation() {
        return new JiraUpdateTracingInstrumentation() {
            @Override
            public void logJiraPollResult(CorrelationTuple correlationTuple, Set<String> set) {
                if (set.size() > 0) log.info("Jira poll result: {}", String.join(",", set));
            }

            @Override
            public void logJiraUpdateResult(CorrelationTuple correlationTuple, Set<String> set) {
                if (set.size() > 0) log.info("Jira update result: {}", String.join(",", set));
            }
        };
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
            FileReader reader = new FileReader(new File("./jirauserstudy.properties"));
            props.load(reader);
            return props;
        } catch (IOException e1) {
            log.info("No properties file in default location (same directory as JAR) found! Using default props.");
            try {
                InputStream inputStream = new ClassPathResource("jirauserstudy.properties").getInputStream();
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
