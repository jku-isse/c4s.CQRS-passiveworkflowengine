package impactassessment;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaUserArtifact;
import artifactapi.jira.IJiraArtifact;
import at.jku.designspace.sdk.clientservice.IDesignspaceChangeSubscriber;
import at.jku.designspace.sdk.clientservice.InstanceService;
import at.jku.designspace.sdk.clientservice.PolarionInstanceService;
import at.jku.designspace.sdk.clientservice.Service;
import at.jku.designspace.sdk.clientservice.exceptions.NotFoundException;
import at.jku.designspace.sdk.clientservice.exceptions.TimeOutException;
import at.jku.designspace.sdk.clientservice.interfaces.IInstanceService;
import at.jku.designspace.sdk.jira.JiraArtifact;
import at.jku.designspace.sdk.polarion.PolarionArtifact;
import at.jku.isse.designspace.sdk.core.DesignSpace;
import at.jku.isse.designspace.sdk.core.model.Instance;
import at.jku.isse.designspace.sdk.core.model.User;
import at.jku.isse.designspace.sdk.core.model.Workspace;
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
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;

import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.demo.Basic1Artifacts;
import impactassessment.artifactconnector.demo.DemoService;
import impactassessment.artifactconnector.designspace.DesignspaceChangeSubscriber;
import impactassessment.artifactconnector.jama.IJamaService;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.artifactconnector.jira.JiraJsonService;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.artifactconnector.usage.HibernatePerProcessArtifactUsagePersistor;
import impactassessment.artifactconnector.usage.InMemoryPerProcessArtifactUsagePersistor;
import impactassessment.artifactconnector.usage.PerProcessArtifactUsagePersistor;
import impactassessment.command.CollectingGatewayProxyFactory;
import impactassessment.command.DefaultGatewayProxyFactory;
import impactassessment.command.IGatewayProxyFactory;
import impactassessment.query.ChangeEventProcessor;
import impactassessment.query.Event2JsonProcessor;
import impactassessment.query.EventList2Forwarder;
import impactassessment.query.IHistoryLogEventLogger;
import impactassessment.query.JsonFileHistoryLogEventLogger;
import impactassessment.query.NoOpHistoryLogEventLogger;
import impactassessment.query.Replayer;
import impactassessment.registry.IRegisterService;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.hibernate.SessionFactory;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;



@Configuration
@Slf4j
public class SpringConfig {

    @Autowired
    private Environment env;
    
    //------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------PROJECT COMPONENTS-----------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

//    @Bean
//    @Primary
//    @ConditionalOnExpression("(${jira.live.enabled:true} or ${jira.demo.enabled:true}) and ${jama.enabled:true}")
//    public IArtifactRegistry getIArtifactRegistry(IJiraService jiraService, IJamaService jamaService) {
//        IArtifactRegistry artifactRegistry = new ArtifactRegistry();
//        artifactRegistry.register(jamaService);
//        artifactRegistry.register(jiraService);
//        return artifactRegistry;
//    }
//
//    @Bean
//    @ConditionalOnExpression("${jira.live.enabled:true} or ${jira.demo.enabled:true}")
//    public IArtifactRegistry getIArtifactRegistryOnlyJira(IJiraService jiraService) {
//        IArtifactRegistry artifactRegistry = new ArtifactRegistry();
//        artifactRegistry.register(jiraService);
//        return artifactRegistry;
//    }
//
//    @Bean
//    @ConditionalOnExpression("${jama.enabled:true}")
//    public IArtifactRegistry getIArtifactRegistryOnlyJama(IJamaService jamaService) {
//        IArtifactRegistry artifactRegistry = new ArtifactRegistry();
//        artifactRegistry.register(jamaService);
//        return artifactRegistry;
//    }
//
//    @Bean
//    @ConditionalOnExpression("not(${jira.live.enabled:true} and ${jira.demo.enabled:true} and ${jama.enabled:true})")
//    public IArtifactRegistry getEmptyIArtifactRegistry() {
//        return new ArtifactRegistry();
//    }

    @Bean
    public IRegisterService getIRegisterService(WorkflowDefinitionRegistry registry, Replayer replayer) {
        return new LocalRegisterService(registry, replayer);
    }

//    @Bean(name="pollIntervalInMinutes")
//    public String pollInterval() {
//        return env.getProperty("pollIntervalInMinutes");
//    }

    @Bean
    @Scope("singleton")
    public IHistoryLogEventLogger getHistoryLogEventLogger() {
    	//IHistoryLogEventLogger lel = new NoOpHistoryLogEventLogger();
    	IHistoryLogEventLogger lel = new JsonFileHistoryLogEventLogger("./");
    	
    	return lel;
    }

    @Bean
    @Scope("singleton")
    public ChangeEventProcessor getEvent2JsonProcessor(EventList2Forwarder el2f, IHistoryLogEventLogger lel) {
    	Event2JsonProcessor cep = new Event2JsonProcessor(lel);
    	el2f.registerProcessor(cep);
    	log.info("ChangeEventProcessor - using "+lel.getClass().getSimpleName());
    	return cep;
    }
    
    @Bean
    @Scope("singleton")
    public IArtifactRegistry getArtifactRegistry(IInstanceService<PolarionArtifact> polarion, IJamaService jamaS, IJiraService jiraS, DemoService ds, ChangeEventProcessor cep) {
        IArtifactRegistry registry = new ArtifactRegistry();
        registry.register(polarion);
        registry.register(jamaS);
        registry.register(jiraS);        
        registry.register(ds);
        // cep required to make sure its loaded
        return registry;
    }
    
    @Bean
    public DemoService getDemoArtifactService() {
    	DemoService ds = new DemoService();
    	Basic1Artifacts.initServiceWithReq(ds);
    	return ds;
    }
    
    @Bean
    @Primary
    @ConditionalOnExpression("${polarion.enabled:false}")
    public IInstanceService<PolarionArtifact> getPolarionService(IDesignspaceChangeSubscriber dcs) {
    	User user = DesignSpace.registerUser("felix"); //TODO: make this configurable
    	PolarionInstanceService  polarionService = new PolarionInstanceService(user, Service.POLARION, "ArtifactConnector");
	    polarionService.addChangeSubscriber(dcs);
    	return polarionService;
    }
                   
    @Bean
 //   @ConditionalOnExpression("${polarion.enabled:false} == false")
    public IInstanceService<PolarionArtifact> getPolarionServiceMock() {
    	return new IInstanceService<PolarionArtifact>() {

			@Override
			public boolean provides(String type) {
				// NOTHING, thus provide nothing
				return false;
			}

			@Override
			public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void injectArtifactService(IArtifact artifact, String workflowId) {
				//NOOP
			}

			@Override
			public void deleteDataScope(String scopeId) {
				//NOOP
			}

			@Override
			public Optional<PolarionArtifact> get(String id, String workflow)
					 {
				// noop
				return Optional.empty();
			}

			@Override
			public Optional<PolarionArtifact> get(String id)  {
				// NOOP
				return null;
			}

			@Override
			public Workspace getWorkspace() {
				// NOOP
				return null;
			}

			@Override
			public Optional<Instance> getInstance(String id) {
				//NOOP
				return Optional.empty();
			}

			@Override
			public Optional<Instance> createServerRequest(String requestId, Service service, String artifactIdentifier)
					throws NotFoundException, TimeOutException {
				// NOOP
				return Optional.empty();
			}

			@Override
			public void putIntoCache(String key, IArtifact artifact) {
				//NOOP
			}

			@Override
			public Optional<IArtifact> searchInCache(String key) {
				return null;
			}

			@Override
			public Service getServiceType() {
				return null;
			}

            @Override
            public void addChangeSubscriber(IDesignspaceChangeSubscriber iDesignspaceChangeSubscriber) {

            }

            @Override
            public void removeChangeSubscriber(IDesignspaceChangeSubscriber iDesignspaceChangeSubscriber) {

            }

        };
    }
    
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------AXON--------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------


    @Bean
    public IGatewayProxyFactory getGatewayProxyFactory(CommandGateway gw) {
		//return new DefaultGatewayProxyFactory(gw);
    	return new CollectingGatewayProxyFactory(gw);
    }
    
    // SETUP TOKEN DB:
    
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
    @ConditionalOnExpression("${jira.live.enabled:false}")
    public IJiraService getJiraService(IJiraInstance jiraInstance, JiraChangeSubscriber jiraChangeSubscriber) {
        return new JiraService(jiraInstance, jiraChangeSubscriber);
    }

    @Bean
    @Primary
    @ConditionalOnExpression("${jira.live.enabled:false}")
    public MonitoringScheduler getJiraMonitoringScheduler(IJiraInstance jiraInstance, IssueCache issueCache) {
        String minutes = env.getProperty("pollIntervalInMinutes");
        ChangeStreamPoller changeStreamPoller = new ChangeStreamPoller(Integer.parseInt(minutes));        
        changeStreamPoller.setJi(jiraInstance);
        changeStreamPoller.setCache(issueCache);
        changeStreamPoller.initLastCacheRefresh();
        MonitoringScheduler scheduler = new MonitoringScheduler();
        scheduler.registerAndStartTask(changeStreamPoller);
        return scheduler;
    }

    @Bean
    @Primary
    @ConditionalOnExpression("${jira.live.enabled:false}")
    public IJiraInstance getJiraInstance(IssueCache issueCache, ChangeSubscriber changeSubscriber, MonitoringState monitoringState) {
        return new JiraInstance(issueCache, changeSubscriber, monitoringState);
    }

    
    @Bean(name="jira")
    @ConditionalOnExpression("${jira.live.enabled:false}")
    public PerProcessArtifactUsagePersistor getHibernatePerProcessArtifactUsagePersistor() {
    	SessionFactory sf = impactassessment.artifactconnector.usage.ConnectionBuilder.createConnection(
                env.getProperty("mysqlDBuser"),
                env.getProperty("mysqlDBpassword"),
                env.getProperty("mysqlURL")+"jiracache?serverTimezone=UTC"
				);
    	return new HibernatePerProcessArtifactUsagePersistor(sf);
    }

    @Bean(name="jira")
    @ConditionalOnExpression("${jama.enabled:false} == false")
    public PerProcessArtifactUsagePersistor getJiraInMemoryPerProcessArtifactUsagePersistor() {
        return new InMemoryPerProcessArtifactUsagePersistor();
    }
    
//    @Bean(name="jira")    
//    public PerProcessArtifactUsagePersistor getInMemoryPerProcessArtifactUsagePersistor() {    	
//    	return new InMemoryPerProcessArtifactUsagePersistor();
//    }
    
    @Bean
    @Primary
    @ConditionalOnExpression("${jira.live.enabled:false}")
    public IssueCache getJiraCache() {
    	SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
                env.getProperty("mysqlDBuser"),
                env.getProperty("mysqlDBpassword"),
                env.getProperty("mysqlURL")+"jiracache?serverTimezone=UTC"
				);
    	return new c4s.jiralightconnector.hibernate.HibernateBackedCache(sf);
     }
    
    @Bean
    @Primary
    @ConditionalOnExpression("${jira.live.enabled:false}")
    public MonitoringState getJiraMonitoringState() {
//        return new InMemoryMonitoringState();
        SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
                env.getProperty("mysqlDBuser"),
                env.getProperty("mysqlDBpassword"),
                env.getProperty("mysqlURL")+"jiracache?serverTimezone=UTC"
        );
        return new HibernateBackedMonitoringState(sf);
    }

    @Bean
    @Primary
    @ConditionalOnExpression("${jira.live.enabled:false}")
    public JiraRestClient getJiraRestClient() {
        String uri = env.getProperty("jiraServerURI");
        String username = env.getProperty("jiraConnectorUsername");
        String pw = env.getProperty("jiraConnectorPassword");
        log.info("Using default AsynchronousJiraRestClientFactory");
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
    @ConditionalOnExpression("${jira.demo.enabled:false}")
    public IJiraService getJiraJsonService() {
        return new JiraJsonService();
    }

    @Bean
    public MonitoringScheduler getEmptyJiraMonitoringScheduler() {
        return new MonitoringScheduler(); // empty scheduler without pollers
    }

    //------------------------------------------------------------------------------------------------------------------
    //---------------------------------------------JIRA via Designspace-------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    @Bean
    @ConditionalOnExpression("${jira.designspace.enabled:false}")
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
    
    
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------JAMA--------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    // possibility to use cached jama dump in a couch db (for development purposes only, don't use in a release!)
    private final boolean USE_DEV_COUCH_DB_FOR_JAMA = false; // FIXME: MUST BE >>>false<<< IN PRODUCTION BUILD!!!

    @Bean
    @Primary
    @ConditionalOnExpression("${jama.enabled:false}")
    public IJamaService getJamaService(JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        return new JamaService(jamaInstance, jamaChangeSubscriber);
    }

    @Bean
    //@ConditionalOnExpression("${jama.enabled} == false")
    public IJamaService getJamaServiceMock() {
        return new IJamaService(){

			@Override
			public boolean provides(String type) {
				// NOOP nothing provided, so null
				return false;
			}

			@Override
			public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
				// NOOP
				return Optional.empty();
			}

			@Override
			public void injectArtifactService(IArtifact artifact, String workflowId) {
				// NOOP
			}

			@Override
			public void deleteDataScope(String scopeId) {
				// NOOP
			}

			@Override
			public Optional<IJamaArtifact> get(Integer id, String workflow) {
				// NOOP
				return Optional.empty();
			}

			@Override
			public Optional<IJamaArtifact> get(Integer id) {
				// NOOP
				return Optional.empty();
			}

			@Override
			public IJamaArtifact convert(JamaItem item) {
				// noop
				return null;
			}

			@Override
			public IJamaProjectArtifact convertProject(JamaProject proj) {
				// noop
				return null;
			}

			@Override
			public IJamaUserArtifact convertUser(JamaUser user) {
				// noop
				return null;
			}

			@Override
			public String getJamaServerUrl(JamaItem jamaItem) {
				// noop
				return null;
			}};
    }
    
    @Bean
    @Primary
    @ConditionalOnExpression("${jama.enabled:false}")
    public c4s.jamaconnector.MonitoringScheduler getJamaMonitoringScheduler(CacheStatus status, JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber, JamaUpdateTracingInstrumentation jamaUpdateTracingInstrumentation) {
        c4s.jamaconnector.MonitoringScheduler scheduler = new c4s.jamaconnector.MonitoringScheduler();
        String projectIds = env.getProperty("jamaProjectIds");
        String[] ids = projectIds.split(",");
        for (String id : ids) {
            c4s.jamaconnector.ChangeStreamPoller changeStreamPoller = new c4s.jamaconnector.ChangeStreamPoller(Integer.parseInt(id), status);
            changeStreamPoller.setInterval(Integer.parseInt(env.getProperty("pollIntervalInMinutes")));
            changeStreamPoller.setJi(jamaInstance);
            changeStreamPoller.setJamaChangeSubscriber(jamaChangeSubscriber);
            changeStreamPoller.setJamaUpdateTracingInstrumentation(jamaUpdateTracingInstrumentation);
            scheduler.registerAndStartTask(changeStreamPoller);
        }
        return scheduler;
    }    

    @Bean
    public c4s.jamaconnector.MonitoringScheduler getEmptyJamaMonitoringScheduler() {
        return new c4s.jamaconnector.MonitoringScheduler(); // empty scheduler without pollers
    }


    @Bean
    @ConditionalOnExpression("${jama.enabled:false}")
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
    @ConditionalOnExpression("${jama.enabled:false}")
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
                    env.getProperty("mysqlURL")+"jamacache?serverTimezone=UTC"
            );
            jamaCache = new HibernateBackedCache(sf);
        }
        return jamaCache;
    }

    @Bean(name="jama")
    @ConditionalOnExpression("${jama.enabled:false}")
    public PerProcessArtifactUsagePersistor getJamaHibernatePerProcessArtifactUsagePersistor() {
    	SessionFactory sf = impactassessment.artifactconnector.usage.ConnectionBuilder.createConnection(
                env.getProperty("mysqlDBuser"),
                env.getProperty("mysqlDBpassword"),
                env.getProperty("mysqlURL")+"jamacache?serverTimezone=UTC"
				);
    	return new HibernatePerProcessArtifactUsagePersistor(sf);
    }
    
    @Bean(name="jama")    
    @ConditionalOnExpression("${jama.enabled:false} == false")
    public PerProcessArtifactUsagePersistor getJamaInMemoryPerProcessArtifactUsagePersistor() {
    	return new InMemoryPerProcessArtifactUsagePersistor();
    }
    
    @Bean
    @ConditionalOnExpression("${jama.enabled:false}")
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
    @ConditionalOnExpression("${jama.enabled:false}")
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
