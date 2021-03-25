package impactassessment;

import artifactapi.IArtifactRegistry;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.OfflineHttpClientMock;
import c4s.jamaconnector.analytics.JamaUpdateTracingInstrumentation;
import c4s.jamaconnector.cache.CacheStatus;
import c4s.jamaconnector.cache.hibernate.HibernateBackedCache;
import c4s.jamaconnector.cache.hibernate.HibernateCacheStatus;
import c4s.jamaconnector.cache.*;
import c4s.jiralightconnector.*;
import c4s.jiralightconnector.analytics.JiraUpdateTracingInstrumentation;
import c4s.jiralightconnector.hibernate.HibernateBackedMonitoringState;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.httpconnection.ApacheHttpClient;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.IJamaService;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.artifactconnector.jira.JiraJsonService;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.query.Replayer;
import impactassessment.registry.IRegisterService;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;

import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.hibernate.SessionFactory;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import javax.sql.DataSource;


@Configuration
@Slf4j
public class SpringConfig {

    private Properties props = null; //fetched on first access

    // default should be true for all flags here:
    private final boolean USE_MY_SQL_CACHE = true; // if false the CouchDB cache will be used
    private final boolean IS_JAMA_INSTANCE_ONLINE = true;
    private final boolean USE_HIBERNATE_MONITORING_STATE = true;
    private final int AXON_SNAPSHOT_THRESHOLD = 10;

    @Bean
    public SnapshotTriggerDefinition workflowSnapshotTrigger(Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, AXON_SNAPSHOT_THRESHOLD);
    }

    @Bean
    @Scope("singleton")
    public IRegisterService getIRegisterService(WorkflowDefinitionRegistry registry, Replayer replayer) {
        return new LocalRegisterService(registry, replayer);
    }

    @Bean
    @Scope("singleton")
    public IArtifactRegistry getArtifactRegistry(IJamaService jamaService, IJiraService jiraService) {
        IArtifactRegistry registry = new ArtifactRegistry();
        registry.register(jamaService);
        registry.register(jiraService);
        return registry;
    }
    
    // SETUP TOKEN DB:
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
       LocalContainerEntityManagerFactoryBean em 
         = new LocalContainerEntityManagerFactoryBean();
       em.setDataSource(dataSource());
       em.setPackagesToScan(new String[] { "org.axonframework.eventsourcing.eventstore.jpa", "org.axonframework.eventhandling.tokenstore.jpa" });

       JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
       em.setJpaVendorAdapter(vendorAdapter);
       em.setJpaProperties(additionalProperties());

       return em;
    }
    
    @Bean
    public DataSource dataSource(){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(getProp("spring.datasource.url"));
        dataSource.setUsername( getProp("spring.datasource.username") );
        dataSource.setPassword( getProp("spring.datasource.password") );
        return dataSource;
    }
    
    Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", getProp("spring.jpa.hibernate.ddl-auto", "update"));
     //   properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
           
        return properties;
    }

    @Bean(name="pollIntervalInMinutes")
    public String pollInterval() {
        return getProp("pollIntervalInMinutes");
    }

    // --------------- JIRA ---------------

    @Bean
    @Scope("singleton")
    public IJiraService getJiraService(JiraInstance jiraInstance, JiraChangeSubscriber jiraChangeSubscriber) {
        if (getProp("jiraDemo", "").equals("true")) {
            return new JiraJsonService();
        } else {
            // connects directly to a Jira server
            return new JiraService(jiraInstance, jiraChangeSubscriber);
        }
    }

    @Bean
    @Scope("singleton")
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
    @Scope("singleton")
    public IssueCache getJiraCache() {
    	SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
				getProp("mysqlDBuser"),
				getProp("mysqlDBpassword"),
				getProp("mysqlURL")+"jiracache"				
				);
    	return new c4s.jiralightconnector.hibernate.HibernateBackedCache(sf);
     }
    
    @Bean
    @Scope("singleton")

    public MonitoringState getJiraMonitoringState() {
        MonitoringState ms;
        if (USE_HIBERNATE_MONITORING_STATE) {
            SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
                    getProp("mysqlDBuser"),
                    getProp("mysqlDBpassword"),
                    getProp("mysqlURL")+"jiracache"
            );
            ms = new HibernateBackedMonitoringState(sf);
        } else {
            ms = new InMemoryMonitoringState();
        }
    	return ms;
    }

    @Bean
    @Scope("singleton")
    public JiraRestClient getJiraRestClient() {
        String uri =  getProp("jiraServerURI");
        String username =  getProp("jiraConnectorUsername");
        String pw =  getProp("jiraConnectorPassword");
        return (new AsynchronousJiraRestClientFactory()).createWithBasicHttpAuthentication(URI.create(uri), username, pw);
    }

    @Bean
    @Scope("singleton")
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
    @Scope("singleton")
    public IJamaService getJamaService(JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        return new JamaService(jamaInstance, jamaChangeSubscriber);
    }

    @Bean
    @Scope("singleton")
    public c4s.jamaconnector.MonitoringScheduler getJamaMonitoringScheduler(CacheStatus status, JamaInstance jamaInstance, JamaUpdateTracingInstrumentation jamaUpdateTracingInstrumentation) {
        c4s.jamaconnector.MonitoringScheduler scheduler = new c4s.jamaconnector.MonitoringScheduler();
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
    @Scope("singleton")
    public CacheStatus getJamaCacheStatus(JamaCache cache) {
        CacheStatus cacheStatus;
        if (USE_MY_SQL_CACHE) {
            cacheStatus = new HibernateCacheStatus((HibernateBackedCache)cache);
        } else {
            cacheStatus = new CouchDBCacheStatus(cache);
        }
    	return cacheStatus;
    }

    @Bean
    @Scope("singleton")
    public JamaCache getJamaCache() {
        JamaCache jamaCache;
        if (USE_MY_SQL_CACHE) {
            SessionFactory sf = c4s.jamaconnector.cache.hibernate.ConnectionBuilder.createConnection(
                    getProp("mysqlDBuser"),
                    getProp("mysqlDBpassword"),
                    getProp("mysqlURL")+"jamacache"
            );
            jamaCache = new HibernateBackedCache(sf);
        } else {
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
            jamaCache = new CouchDBJamaCache(new CouchDbClient(dbprops));
        }
        return jamaCache;
    }

    @Bean
    @Scope("singleton")
    public JamaInstance getOnlineJamaInstance(JamaCache cache) {
        JamaInstance jamaInst;
        if (IS_JAMA_INSTANCE_ONLINE) {
            JamaConfig jamaConf = new JamaConfig();
            jamaConf.setJson(new CachingJsonHandler(cache));
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

            jamaInst = new JamaInstance(jamaConf, false);
            cache.setJamaInstance(jamaInst);
            jamaInst.setResourcePool(new CachedResourcePool(cache));
            jamaInst.enableAnonymizing();
        } else {
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

            jamaInst = new JamaInstance(jamaConf, true);
            cache.setJamaInstance(jamaInst);
            jamaInst.setResourcePool(new CachedResourcePool(cache));
            return jamaInst;
        }
        return jamaInst;
    }

    @Bean
    @Scope("singleton")
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
            log.error("No properties file found.");
        }
        return props;
    }


}
