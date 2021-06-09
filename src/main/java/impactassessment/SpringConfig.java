package impactassessment;

import artifactapi.IArtifactRegistry;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.OfflineHttpClientMock;
import c4s.jamaconnector.analytics.JamaUpdateTracingInstrumentation;
import c4s.jamaconnector.cache.CacheStatus;
import c4s.jamaconnector.cache.*;
import c4s.jamaconnector.cache.hibernate.HibernateBackedCache;
import c4s.jamaconnector.cache.hibernate.HibernateCacheStatus;
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
import impactassessment.featureflags.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;


@Configuration
@Slf4j
public class SpringConfig {

    @Autowired
    private Environment env;


    //------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------PROJECT COMPONENTS-----------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    @Bean
    @Primary
    @IfJiraLiveOrDemo
    @IfJama
    public IArtifactRegistry getIArtifactRegistry(IJiraService jiraService, IJamaService jamaService) {
        IArtifactRegistry artifactRegistry = new ArtifactRegistry();
        artifactRegistry.register(jamaService);
        artifactRegistry.register(jiraService);
        return artifactRegistry;
    }

    @Bean
    @IfJiraLiveOrDemo
    public IArtifactRegistry getIArtifactRegistryOnlyJira(IJiraService jiraService) {
        IArtifactRegistry artifactRegistry = new ArtifactRegistry();
        artifactRegistry.register(jiraService);
        return artifactRegistry;
    }

    @Bean
    @IfJama
    public IArtifactRegistry getIArtifactRegistryOnlyJama(IJamaService jamaService) {
        IArtifactRegistry artifactRegistry = new ArtifactRegistry();
        artifactRegistry.register(jamaService);
        return artifactRegistry;
    }

    @Bean
    @IfNone
    public IArtifactRegistry getEmptyIArtifactRegistry() {
        return new ArtifactRegistry();
    }

    @Bean
    public IRegisterService getIRegisterService(WorkflowDefinitionRegistry registry, Replayer replayer) {
        return new LocalRegisterService(registry, replayer);
    }

//    @Bean(name="pollIntervalInMinutes")
//    public String pollInterval() {
//        return env.getProperty("pollIntervalInMinutes");
//    }



    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------AXON--------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    @Bean
    public SnapshotTriggerDefinition workflowSnapshotTrigger(Snapshotter snapshotter) {
        int AXON_SNAPSHOT_THRESHOLD = 10;
        return new EventCountSnapshotTriggerDefinition(snapshotter, AXON_SNAPSHOT_THRESHOLD);
    }

    // token database
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
       LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
       em.setDataSource(dataSource());
       em.setPackagesToScan(new String[] { "org.axonframework.eventsourcing.eventstore.jpa", "org.axonframework.eventhandling.tokenstore.jpa" });
       JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
       em.setJpaVendorAdapter(vendorAdapter);
       em.setJpaProperties(additionalProperties());
       return em;
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto", "update"));
        //   properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
        return properties;
    }

    // token database
    @Bean
    public DataSource dataSource(){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(env.getProperty("spring.datasource.url"));
        dataSource.setUsername(env.getProperty("spring.datasource.username") );
        dataSource.setPassword(env.getProperty("spring.datasource.password") );
        return dataSource;
    }
    


    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------JIRA--------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    @Bean
    @Primary
    @IfJiraLive
    public IJiraService getJiraService(IJiraInstance jiraInstance, JiraChangeSubscriber jiraChangeSubscriber) {
        return new JiraService(jiraInstance, jiraChangeSubscriber);
    }

    @Bean
    @Primary
    @IfJiraLive
    public MonitoringScheduler getJiraMonitoringScheduler(IJiraInstance jiraInstance, IssueCache issueCache) {
        String minutes = env.getProperty("pollIntervalInMinutes");
        String s = env.getProperty("pollIntervalInMinutes");
        System.out.println("------> "+minutes+" ------ "+s);
        ChangeStreamPoller changeStreamPoller = new ChangeStreamPoller(Integer.parseInt(minutes));
        changeStreamPoller.setJi(jiraInstance);
        changeStreamPoller.setCache(issueCache);
        MonitoringScheduler scheduler = new MonitoringScheduler();
        scheduler.registerAndStartTask(changeStreamPoller);
        return scheduler;
    }

    @Bean
    @Primary
    @IfJiraLive
    public IJiraInstance getJiraInstance(IssueCache issueCache, ChangeSubscriber changeSubscriber, MonitoringState monitoringState) {
        return new JiraInstance(issueCache, changeSubscriber, monitoringState);
    }

    @Bean
    @Primary
    @IfJiraLive
    public IssueCache getJiraCache() {
    	SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
                env.getProperty("mysqlDBuser"),
                env.getProperty("mysqlDBpassword"),
                env.getProperty("mysqlURL")+"jiracache"
				);
    	return new c4s.jiralightconnector.hibernate.HibernateBackedCache(sf);
     }
    
    @Bean
    @Primary
    @IfJiraLive
    public MonitoringState getJiraMonitoringState() {
//        return new InMemoryMonitoringState();
        SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
                env.getProperty("mysqlDBuser"),
                env.getProperty("mysqlDBpassword"),
                env.getProperty("mysqlURL")+"jiracache"
        );
        return new HibernateBackedMonitoringState(sf);
    }

    @Bean
    @Primary
    @IfJiraLive
    public JiraRestClient getJiraRestClient() {
        String uri = env.getProperty("jiraServerURI");
        String username = env.getProperty("jiraConnectorUsername");
        String pw = env.getProperty("jiraConnectorPassword");
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



    //------------------------------------------------------------------------------------------------------------------
    //---------------------------------------------JIRA DEMO------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    @Bean
    @IfJiraDemo
    public IJiraService getJiraJsonService() {
        return new JiraJsonService();
    }

    @Bean
    public MonitoringScheduler getEmptyJiraMonitoringScheduler() {
        return new MonitoringScheduler(); // empty scheduler without pollers
    }



    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------JAMA--------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    // possibility to use cached jama dump in a couch db (for development purposes only, don't use in a release!)
    private final boolean USE_DEV_COUCH_DB_FOR_JAMA = false; // FIXME: MUST BE >>>false<<< IN PRODUCTION BUILD!!!

    @Bean
    @IfJama
    public IJamaService getJamaService(JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        return new JamaService(jamaInstance, jamaChangeSubscriber);
    }

    @Bean
    @Primary
    @IfJama
    public c4s.jamaconnector.MonitoringScheduler getJamaMonitoringScheduler(CacheStatus status, JamaInstance jamaInstance, JamaUpdateTracingInstrumentation jamaUpdateTracingInstrumentation) {
        c4s.jamaconnector.MonitoringScheduler scheduler = new c4s.jamaconnector.MonitoringScheduler();
        String projectIds = env.getProperty("jamaProjectIds");
        String[] ids = projectIds.split(",");
        for (String id : ids) {
            c4s.jamaconnector.ChangeStreamPoller changeStreamPoller = new c4s.jamaconnector.ChangeStreamPoller(Integer.parseInt(id), status);
            changeStreamPoller.setInterval(Integer.parseInt(env.getProperty("pollIntervalInMinutes")));
            changeStreamPoller.setJi(jamaInstance);
            changeStreamPoller.setJamaUpdateTracingInstrumentation(jamaUpdateTracingInstrumentation);
            scheduler.registerAndStartTask(changeStreamPoller);
        }
        return scheduler;
    }

    @Bean
    public c4s.jamaconnector.MonitoringScheduler getEmptyJamaMonitoringScheduler() {
        return new c4s.jamaconnector.MonitoringScheduler(); // empty scheduler without pollers
    }

//    @Bean
//    public JamaConnector getJamaConnector(AutowireCapableBeanFactory beanFactory) {
//        JamaConnector jamaConn = new OfflineJamaConnector(1); // pollInterval is not used
//        beanFactory.autowireBean(jamaConn);
//        return jamaConn;
//    }

    @Bean
    @IfJama
    public CacheStatus getJamaCacheStatus(JamaCache cache) {
        CacheStatus cacheStatus;
        if (USE_DEV_COUCH_DB_FOR_JAMA) {
            cacheStatus = new CouchDBCacheStatus(cache);
        } else {
            cacheStatus = new HibernateCacheStatus((HibernateBackedCache)cache);
        }
    	return cacheStatus;
    }

    @Bean
    @IfJama
    public JamaCache getJamaCache() {
        JamaCache jamaCache;
        if (USE_DEV_COUCH_DB_FOR_JAMA) {
            CouchDbProperties dbprops = new CouchDbProperties()
                    .setDbName(env.getProperty("jamaCacheCouchDBname", "jamaitems3"))
                    .setCreateDbIfNotExist(true)
                    .setProtocol("http")
                    .setHost(env.getProperty("couchDBip", "localhost"))
                    .setPort(Integer.parseInt(env.getProperty("couchDBport", "5984")))
                    .setUsername(env.getProperty("jamaCacheCouchDBuser","admin"))
                    .setPassword(env.getProperty("jamaCacheCouchDBpassword","password"))
                    .setMaxConnections(100)
                    .setConnectionTimeout(0);
            jamaCache = new CouchDBJamaCache(new CouchDbClient(dbprops));
        } else {
            SessionFactory sf = c4s.jamaconnector.cache.hibernate.ConnectionBuilder.createConnection(
                    env.getProperty("mysqlDBuser"),
                    env.getProperty("mysqlDBpassword"),
                    env.getProperty("mysqlURL")+"jamacache"
            );
            jamaCache = new HibernateBackedCache(sf);
        }
        return jamaCache;
    }

    @Bean
    @IfJama
    public JamaInstance getJamaInstance(JamaCache cache) {
        JamaInstance jamaInst;
        if (USE_DEV_COUCH_DB_FOR_JAMA) {
            // run jama instance offline
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
        } else {
            JamaConfig jamaConf = new JamaConfig();
            jamaConf.setJson(new CachingJsonHandler(cache));
            jamaConf.setApiKey(env.getProperty("jamaOptionalKey", "SUPERSECRETKEY"));
            String url = env.getProperty("jamaServerURI");
            jamaConf.setBaseUrl(url);
            jamaConf.setResourceTimeOut(Integer.MAX_VALUE);
            jamaConf.setOpenUrlBase(url);
            jamaConf.setUsername(env.getProperty("jamaUser"));
            jamaConf.setPassword(env.getProperty("jamaPassword"));
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
        }
        return jamaInst;
    }

    @Bean
    @IfJama
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

}
