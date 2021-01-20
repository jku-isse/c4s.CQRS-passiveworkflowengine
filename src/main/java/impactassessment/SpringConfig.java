package impactassessment;

import artifactapi.IArtifactRegistry;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.analytics.JamaUpdateTracingInstrumentation;
import c4s.jamaconnector.cache.CacheStatus;
import c4s.jamaconnector.cache.*;
import c4s.jiralightconnector.*;
import c4s.jiralightconnector.analytics.JiraUpdateTracingInstrumentation;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.httpconnection.ApacheHttpClient;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.artifactconnector.jira.MockedJiraCache;
import impactassessment.registry.IRegisterService;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class SpringConfig {

    private Properties props = null;

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

    @Bean
    public JiraService getJiraService(JiraInstance jiraInstance, JiraChangeSubscriber jiraChangeSubscriber) {
        // connects directly to a Jira server
        return new JiraService(jiraInstance, jiraChangeSubscriber);
    }

    @Bean
    public MonitoringScheduler getJiraMonitoringScheduler(JiraInstance jiraInstance, IssueCache issueCache) {
        String minutes =  getProp("pollIntervalInMinutes");
        ChangeStreamPoller changeStreamPoller = new ChangeStreamPoller(Integer.parseInt(minutes));
        changeStreamPoller.setJi(jiraInstance);
        changeStreamPoller.setCache(issueCache);
        MonitoringScheduler scheduler = new MonitoringScheduler();
        scheduler.registerAndStartTask(changeStreamPoller);
        return scheduler;
    }


    @Bean
    public JiraInstance getJiraInstance(IssueCache issueCache, ChangeSubscriber changeSubscriber, MonitoringState monitoringState) {
        return new JiraInstance(issueCache, changeSubscriber, monitoringState);
    }

    @Bean
    public IssueCache getJiraCache() {
        return new MockedJiraCache();
    }

    @Bean
    public MonitoringState getMonitoringState() {
        return new InMemoryMonitoringState();
    }

    @Bean
    public JiraRestClient getJiraRestClient() {
        String uri =  getProp("jiraServerURI");
        String username =  getProp("jiraConnectorUsername");
        String pw =  getProp("jiraConnectorPassword");
        return (new AsynchronousJiraRestClientFactory()).createWithBasicHttpAuthentication(URI.create(uri), username, pw);
    }

    @Bean
    public JiraUpdateTracingInstrumentation getJiraUpdateTracingInstrumentation() {
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

    // --------------- JAMA ---------------

    @Bean
    public JamaService getJamaService(JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        return new JamaService(jamaInstance, jamaChangeSubscriber);
    }

    @Bean
    public c4s.jamaconnector.MonitoringScheduler getJamaMonitoringScheduler(JamaCache cache, JamaInstance jamaInstance, JamaUpdateTracingInstrumentation jamaUpdateTracingInstrumentation) {
        c4s.jamaconnector.MonitoringScheduler scheduler = new c4s.jamaconnector.MonitoringScheduler();
        CacheStatus status = new CacheStatus(cache);
        String projectIds =  getProp("jamaProjectIds");
        String[] ids = projectIds.split(",");
        for (String id : ids) {
            c4s.jamaconnector.ChangeStreamPoller changeStreamPoller = new c4s.jamaconnector.ChangeStreamPoller(Integer.parseInt(id), status);
            changeStreamPoller.setInterval(Integer.parseInt(getProp("pollIntervalInMinutes")));
            changeStreamPoller.setJi(jamaInstance);
            changeStreamPoller.setJamaUpdateTracingInstrumentation(jamaUpdateTracingInstrumentation);
            scheduler.registerAndStartTask(changeStreamPoller);
        }
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

//    @Bean
//    public JamaInstance getOfflineJamaInstance(CouchDBJamaCache cache) {
//        JamaConfig jamaConf = new JamaConfig();
//        jamaConf.setJson(new CachingJsonHandler(cache));
//        jamaConf.setApiKey("SUPERSECRETKEY");
//        String url = "http://localhost";
//        jamaConf.setBaseUrl(url);
//        jamaConf.setResourceTimeOut(Integer.MAX_VALUE);
//        jamaConf.setOpenUrlBase(url);
//        jamaConf.setUsername("OFFLINE");
//        jamaConf.setPassword("OFFLINE");
//        jamaConf.setResourceTimeOut(60);
//        jamaConf.setHttpClient(new OfflineHttpClientMock());
//
//        JamaInstance jamaInst = new JamaInstance(jamaConf, true);
//        cache.setJamaInstance(jamaInst);
//        jamaInst.setResourcePool(new CachedResourcePool(cache));
//        return jamaInst;
//    }

    @Bean
    public JamaInstance getOnlineJamaInstance(CouchDBJamaCache cache) {
        JamaConfig jamaConf = new JamaConfig();
        jamaConf.setJson(new CachingJsonHandler(cache));
        jamaConf.setApiKey(getProp("jamaSecretKey"));
        String url = getProp("jamaUrl");
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
        cache.setJamaInstance(jamaInst);
        jamaInst.setResourcePool(new CachedResourcePool(cache));
        return jamaInst;
    }

    @Bean
    public CouchDbClient getCouchDbClient() {
        CouchDbProperties dbprops = new CouchDbProperties()
                .setDbName(getProp("jamaCacheCouchDBname", "jamaitems3"))
                .setCreateDbIfNotExist(true)
                .setProtocol("http")
                .setHost(getProp("couchDBip", "localhost"))
                .setPort(Integer.parseInt(getProp("couchDBport", "5984")))
                .setUsername(getProp("jamaCacheCouchDBuser","admin"))
                .setPassword(getProp("jamaCacheCouchDBpassword","password"))
                .setMaxConnections(100)
                .setConnectionTimeout(0);
        return new CouchDbClient(dbprops);
    }

    @Bean
    public JamaUpdateTracingInstrumentation getUpdateTraceInstrumentation() {
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

    // ----------------------------- Property Access -----------------------------

    private String getProp(String name) {
        return getProp(name, null);
    }

    private String getProp(String name, String defaultValue) {
        if (props == null) {
            props = getProps();
        }
        String value = props.getProperty(name, defaultValue);
        if (value == null) {
            log.error("Required property {} was not found in the application properties!", name);
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
            log.warn("No properties file in default location (same directory as JAR) found! Using default props.");
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
