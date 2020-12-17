package impactassessment;

import artifactapi.IArtifactRegistry;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.*;
import c4s.jamaconnector.analytics.JamaUpdateTracingInstrumentation;
import c4s.jamaconnector.cache.*;
import c4s.jamaconnector.cache.CacheStatus;
import c4s.jiralightconnector.*;
import c4s.jiralightconnector.ChangeStreamPoller;
import c4s.jiralightconnector.InMemoryMonitoringState;
import c4s.jiralightconnector.MonitoringScheduler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.registry.IRegisterService;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Configuration
public class SpringConfig {

    private static final int POLL_INTERVAL_IN_MINUTES = 1; // used for both Jira and Jama

    @Bean
    public IRegisterService getIRegisterService(WorkflowDefinitionRegistry registry) {
        return new LocalRegisterService(registry);
    }

    @Bean
    public IArtifactRegistry getArtifactRegistry(JamaService jamaService, JiraService jiraService) {
        IArtifactRegistry registry = new ArtifactRegistry();
        registry.register(jamaService);
        registry.register(jiraService);
        return registry;
    }

    // --------------- JIRA ---------------

//    @Bean
//    public IJiraArtifactService getJiraArtifactService(JiraChangeSubscriber jiraChangeSubscriber) {
//        // uses JSON image of Jira data in resources folder
//        return new JiraJsonService(jiraChangeSubscriber);
//    }

    @Bean
    public JiraService getJiraService(JiraInstance jiraInstance, JiraChangeSubscriber jiraChangeSubscriber) {
        // connects directly to a Jira server
        return new JiraService(jiraInstance, jiraChangeSubscriber);
    }

    @Bean
    public ChangeStreamPoller getChangeStreampoller() {
        return new ChangeStreamPoller(POLL_INTERVAL_IN_MINUTES);
    }

    @Bean
    public MonitoringScheduler getJiraMonitoringScheduler(ChangeStreamPoller changeStreamPoller) {
        MonitoringScheduler scheduler = new MonitoringScheduler();
        scheduler.registerAndStartTask(changeStreamPoller);
        return scheduler;
    }

    @Bean
    public JiraInstance getJiraInstance(IssueCache issueCache, ChangeSubscriber changeSubscriber, MonitoringState monitoringState) {
        return new JiraInstance(issueCache, changeSubscriber, monitoringState);
    }

    @Bean
    public MonitoringState getMonitoringState() {
        return new InMemoryMonitoringState();
    }

    @Bean
    public JiraRestClient getJiraRestClient() {
        Properties props = getProps();
        String uri =  props.getProperty("jiraServerURI");
        String username =  props.getProperty("jiraConnectorUsername");
        String pw =  props.getProperty("jiraConnectorPassword");
        return (new AsynchronousJiraRestClientFactory()).createWithBasicHttpAuthentication(URI.create(uri), username, pw);
    }

    // --------------- JAMA ---------------

    @Bean
    public JamaService getJamaService(JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        return new JamaService(jamaInstance, jamaChangeSubscriber);
    }

    @Bean
    public c4s.jamaconnector.ChangeStreamPoller getJamaChangeStreamPoller(JamaCache cache, JamaInstance jamaInstance, JamaUpdateTracingInstrumentation jamaUpdateTracingInstrumentation) {
        CacheStatus status = new CacheStatus(cache);
        c4s.jamaconnector.ChangeStreamPoller changeStreamPoller = new c4s.jamaconnector.ChangeStreamPoller(123, status); // TODO not sure about project id
        changeStreamPoller.setJi(jamaInstance);
        changeStreamPoller.setJamaUpdateTracingInstrumentation(jamaUpdateTracingInstrumentation);
        return changeStreamPoller;
    }

    @Bean
    public int intervalInMinutes() {
        return POLL_INTERVAL_IN_MINUTES;
    }

    @Bean
    public c4s.jamaconnector.MonitoringScheduler getJamaMonitoringScheduler(c4s.jamaconnector.ChangeStreamPoller changeStreamPoller) {
        c4s.jamaconnector.MonitoringScheduler scheduler = new c4s.jamaconnector.MonitoringScheduler();
        scheduler.registerAndStartTask(changeStreamPoller);
        return scheduler;
    }

//    @Bean
//    public JamaConnector getJamaConnector(AutowireCapableBeanFactory beanFactory) {
//        JamaConnector jamaConn = new OfflineJamaConnector(1); // pollInterval is not used
//        beanFactory.autowireBean(jamaConn);
//        return jamaConn;
//    }

    @Bean
    public CouchDBJamaCache getCache(CouchDbClient dbClient) {
        return new CouchDBJamaCache(dbClient);
    }

    @Bean
    public JamaInstance getJamaInstance(CouchDBJamaCache cache) {
        JamaConfig jamaConf = new JamaConfig();
        jamaConf.setJson(new CachingJsonHandler(cache));
        jamaConf.setApiKey("SUPERSECRETKEY");
        String url = "http://localhost";
        jamaConf.setBaseUrl(url);
        jamaConf.setResourceTimeOut(Integer.MAX_VALUE);
        jamaConf.setOpenUrlBase(url);
        jamaConf.setUsername("OFFLINE");
        jamaConf.setPassword("OFFLINE");
        jamaConf.setResourceTimeOut(60);
        jamaConf.setHttpClient(new OfflineHttpClientMock());

        JamaInstance jamaInst = new JamaInstance(jamaConf, true);
        cache.setJamaInstance(jamaInst);
        jamaInst.setResourcePool(new CachedResourcePool(cache));
        return jamaInst;
    }

    @Bean
    public CouchDbClient getCouchDbClient() {
        Properties props = getProps();
        CouchDbProperties dbprops = new CouchDbProperties()
                .setDbName(props.getProperty("jiraCacheCouchDBname", "jamaitems3"))
                .setCreateDbIfNotExist(true)
                .setProtocol("http")
                .setHost(props.getProperty("couchDBip", "localhost"))
                .setPort(Integer.parseInt(props.getProperty("couchDBport", "5984")))
                .setUsername(props.getProperty("jiraCacheCouchDBuser","admin"))
                .setPassword(props.getProperty("jiraCacheCouchDBpassword","password"))
                .setMaxConnections(100)
                .setConnectionTimeout(0);
        return new CouchDbClient(dbprops);
    }

    // ------------------------- JAMA MOCKS -------------------------

//    @Bean
//    public ProjectMonitoringState getProjectMonitoringState() {
//        return new ProjectMonitoringState() {
//            @Override
//            public Set<Integer> getMonitoredProjectIds() {
//                return null;
//            }
//
//            @Override
//            public void removeMonitoredProject(Integer integer) {
//
//            }
//
//            @Override
//            public void addMonitoredProject(Integer integer) {
//
//            }
//        };
//    }
//
//    @Bean
//    public ItemMonitoringState getItemMonitoringState() {
//        return new ItemMonitoringState() {
//            @Override
//            public Set<Integer> getMonitoredItemIds() {
//                return null;
//            }
//
//            @Override
//            public boolean removeMonitoredItem(Integer integer) {
//                return false;
//            }
//
//            @Override
//            public void addMonitoredItem(Integer integer) {
//
//            }
//        };
//    }

    @Bean
    public JamaUpdateTracingInstrumentation getUpdateTraceInstrumentation() {
        return new JamaUpdateTracingInstrumentation() {
            @Override
            public void logJamaPollResult(CorrelationTuple correlationTuple, int i, Map<String, Set<Integer>> map) {

            }

            @Override
            public void logJamaUpdateResult(CorrelationTuple correlationTuple, int i, Set<JamaItem> set) {

            }
        };
    }

    private Properties getProps() {
        Properties props = new Properties();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("application.properties").getFile());
            FileReader reader = new FileReader(file);
            props.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }
}
