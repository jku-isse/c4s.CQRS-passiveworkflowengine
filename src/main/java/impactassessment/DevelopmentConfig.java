package impactassessment;

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
import impactassessment.artifactconnector.usage.InMemoryPerProcessArtifactUsagePersistor;
import impactassessment.command.CollectingGatewayProxyFactory;
import impactassessment.command.DefaultGatewayProxy;
import impactassessment.command.DefaultGatewayProxyFactory;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;


public class DevelopmentConfig extends AbstractModule {

    protected Logger log = LogManager.getLogger(DevelopmentConfig.class);

    HibernateBackedCache jamaCache;

    private JamaService jamaS;
    private c4s.jamaconnector.MonitoringScheduler jamaM;
    private CacheStatus jamaStatus;
    private JamaInstance jamaI;
    private JamaUpdateTracingInstrumentation jamaUTI;
    private WorkflowDefinitionRegistry registry;
    private CommandGateway gw;

    private JiraRestClient jrc;
    private c4s.jiralightconnector.hibernate.HibernateBackedCache jiraCache;
    private JiraUpdateTracingInstrumentation juti;
    private JiraChangeSubscriber jiraCS;
    private MonitoringState jiraM;
    private JiraInstance jiraI;
    private ArtifactRegistry artReg;

    WorkflowProjection wfp;
	ProjectionModel pModel;
	IKieSessionService kieS;
	EventList2Forwarder el2f = new EventList2Forwarder();

    public DevelopmentConfig() {
        artReg = new ArtifactRegistry();
        registry = new WorkflowDefinitionRegistry();
        LocalRegisterService lrs = new LocalRegisterService(registry);
        lrs.registerAll();
        gw = new MockCommandGateway(artReg, registry);
        jrc = setupJiraRestClient();
        jiraCache = configJiraCache();
        juti = setupJiraUpdateTracingInstrumentation();
        jiraM = configJiraMonitoringState();
        jiraCS = new JiraChangeSubscriber(gw, new InMemoryPerProcessArtifactUsagePersistor());

        jamaCache = setupJamaCache();
        jamaStatus = setupJamaCacheStatus();
        jamaUTI = setupUpdateTraceInstrumentation();
        jamaI = configOnlineJamaInstance();
        jamaM = setupJamaMonitoringScheduler();
        jamaS = configJamaService();
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
        bind(JiraRestClient.class).toInstance(jrc);
        bind(JiraUpdateTracingInstrumentation.class).toInstance(juti);
        bind(JiraChangeSubscriber.class).toInstance(jiraCS);
        bind(ChangeSubscriber.class).toInstance(jiraCS);
        bind(MonitoringState.class).toInstance(jiraM);
        bind(IssueCache.class).toInstance(jiraCache);
        bind(AnonymizingJiraInstance.class).asEagerSingleton();
        bind(JamaInstance.class).toInstance(jamaI);
        bind(JamaService.class).toInstance(jamaS);
        bind(WorkflowDefinitionRegistry.class).toInstance(registry);
		bind(WorkflowProjection.class).toInstance(wfp);
		bind(IKieSessionService.class).toInstance(kieS);
		bind(ProjectionModel.class).toInstance(pModel);
    }

    private static JiraService configJiraService(JiraInstance jiraI, JiraChangeSubscriber jiraCS) {
        return new JiraService(jiraI, jiraCS);
    }


    private static Injector inj;
    private static IJiraService jiraS;

    public static Injector getInjector() {
        if (inj == null)
            inj = Guice.createInjector(new DevelopmentConfig());
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

    private c4s.jiralightconnector.hibernate.HibernateBackedCache configJiraCache() {
        SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
                getProp("mysqlDBuser"),
                getProp("mysqlDBpassword"),
                getProp("mysqlURL")+"jiracache"
        );
        return new c4s.jiralightconnector.hibernate.HibernateBackedCache(sf);
    }

    private MonitoringState configJiraMonitoringState() {
        SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
                getProp("mysqlDBuser"),
                getProp("mysqlDBpassword"),
                getProp("mysqlURL")+"jiracache"
        );
        return new HibernateBackedMonitoringState(sf);
    }

    private JamaService configJamaService() {
        jamaS = new JamaService(jamaI, new JamaChangeSubscriber(gw, new InMemoryPerProcessArtifactUsagePersistor()));
        return jamaS;
    }

    private CacheStatus setupJamaCacheStatus() {
        HibernateCacheStatus status = new HibernateCacheStatus(jamaCache);
        return status;
    }

    private HibernateBackedCache setupJamaCache() {
        SessionFactory sf = c4s.jamaconnector.cache.hibernate.ConnectionBuilder.createConnection(
                getProp("mysqlDBuser"),
                getProp("mysqlDBpassword"),
                getProp("mysqlURL")+"jamacache"
        );
        return new HibernateBackedCache(sf);
    }

    private c4s.jamaconnector.MonitoringScheduler setupJamaMonitoringScheduler() {
        c4s.jamaconnector.MonitoringScheduler scheduler = new c4s.jamaconnector.MonitoringScheduler();
        String projectIds =  getProp("jamaProjectIds");
        String[] ids = projectIds.split(",");
        for (String id : ids) {
            c4s.jamaconnector.ChangeStreamPoller changeStreamPoller = new c4s.jamaconnector.ChangeStreamPoller(Integer.parseInt(id), jamaStatus);
            changeStreamPoller.setInterval(Integer.parseInt(getProp("pollIntervalInMinutes")));
            changeStreamPoller.setJi(jamaI);
            changeStreamPoller.setJamaUpdateTracingInstrumentation(jamaUTI);
            scheduler.registerAndStartTask(changeStreamPoller);
        }
        return scheduler;
    }

    private JamaInstance configOnlineJamaInstance() {
        JamaConfig jamaConf = new JamaConfig();
        jamaConf.setJson(new CachingJsonHandler(jamaCache));
        jamaConf.setApiKey(getProp("jamaOptionalKey", "SUPERSECRETKEY"));
        String url = getProp("jamaServerURI");
        jamaConf.setBaseUrl(url);
        jamaConf.setResourceTimeOut(Integer.MAX_VALUE);
        jamaConf.setOpenUrlBase(url);
        jamaConf.setUsername(getProp("jamaUser"));
        jamaConf.setPassword(getProp("jamaPassword"));
        jamaConf.setResourceTimeOut(60);
        try {
            jamaConf.setHttpClient(new ApacheHttpClient());
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        JamaInstance jamaInst = new JamaInstance(jamaConf, false);
        jamaInst.enableAnonymizing();
        jamaCache.setJamaInstance(jamaInst);
        jamaInst.setResourcePool(new CachedResourcePool(jamaCache));

        return jamaInst;
    }


    private JamaUpdateTracingInstrumentation setupUpdateTraceInstrumentation() {
        return new JamaUpdateTracingInstrumentation() {
            @Override
            public void logJamaPollResult(CorrelationTuple correlationTuple, int i, Map<String, Set<Integer>> map) {
                if (map.size() > 0) log.info("Jama poll result: {}", String.join(",", map.keySet()));
            }

            @Override
            public void logJamaUpdateResult(CorrelationTuple correlationTuple, int i, Set<JamaItem> set) {
                if (set.size() > 0) log.info("Jama update result: {}", set.stream().map(JamaItem::getDocumentKey).collect(Collectors.joining(",")));
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
            FileReader reader = new FileReader(new File("./application.properties"));
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
