package impactassessment;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.hibernate.SessionFactory;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class DevelopmentConfig extends AbstractModule {

	protected Logger log = LogManager.getLogger(DevelopmentConfig.class);
	@Autowired
	private Environment env;
	
	HibernateBackedCache jamaCache;
	
	private JamaService jamaS;
	private c4s.jamaconnector.MonitoringScheduler jamaM;
	private CacheStatus jamaStatus; 
	private JamaInstance jamaI;
	private JamaUpdateTracingInstrumentation jamaUTI;
	
	private CommandGateway gw;
	
	private JiraRestClient jrc;
	private c4s.jiralightconnector.hibernate.HibernateBackedCache jiraCache;
	private JiraUpdateTracingInstrumentation juti;
	private JiraChangeSubscriber jiraCS;
	private MonitoringState jiraM;
	private JiraInstance jiraI;
	private ArtifactRegistry artReg;


	
	public DevelopmentConfig() {
		artReg = new ArtifactRegistry();
		gw = new MockCommandGateway(artReg);
		jrc = setupJiraRestClient();
		jiraCache = configJiraCache();
		juti = setupJiraUpdateTracingInstrumentation();
		jiraM = configJiraMonitoringState();
		jiraCS = new JiraChangeSubscriber(gw);
		
		jamaCache = setupJamaCache();
		jamaStatus = setupJamaCacheStatus();
		jamaUTI = setupUpdateTraceInstrumentation();
		jamaI = configOnlineJamaInstance();
		jamaM = setupJamaMonitoringScheduler();
		jamaS = configJamaService();
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
        String uri =  env.getProperty("jiraServerURI");
        String username =  env.getProperty("jiraConnectorUsername");
        String pw =  env.getProperty("jiraConnectorPassword");
        return new AnonymizingAsyncJiraRestClientFactory()
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
				env.getProperty("mysqlDBuser"),
				env.getProperty("mysqlDBpassword"),
				env.getProperty("mysqlURL")+"jiracache"
				);
    	return new c4s.jiralightconnector.hibernate.HibernateBackedCache(sf);
     }

    private MonitoringState configJiraMonitoringState() {
    	SessionFactory sf = c4s.jiralightconnector.hibernate.ConnectionBuilder.createConnection(
				env.getProperty("mysqlDBuser"),
				env.getProperty("mysqlDBpassword"),
				env.getProperty("mysqlURL")+"jiracache"
				);
    	return new HibernateBackedMonitoringState(sf);
    }
    
    private JamaService configJamaService() {
		jamaS = new JamaService(jamaI, new JamaChangeSubscriber(gw));
		return jamaS;
	}
	
	private CacheStatus setupJamaCacheStatus() {
    	HibernateCacheStatus status = new HibernateCacheStatus(jamaCache);
    	return status;
    }
    
    private HibernateBackedCache setupJamaCache() {
    	SessionFactory sf = c4s.jamaconnector.cache.hibernate.ConnectionBuilder.createConnection(
				env.getProperty("mysqlDBuser"),
				env.getProperty("mysqlDBpassword"),
				env.getProperty("mysqlURL")+"jamacache"
				);
		return new HibernateBackedCache(sf);
     }
	
	private c4s.jamaconnector.MonitoringScheduler setupJamaMonitoringScheduler() {
        c4s.jamaconnector.MonitoringScheduler scheduler = new c4s.jamaconnector.MonitoringScheduler();
        String projectIds =  env.getProperty("jamaProjectIds");
        String[] ids = projectIds.split(",");
        for (String id : ids) {
            c4s.jamaconnector.ChangeStreamPoller changeStreamPoller = new c4s.jamaconnector.ChangeStreamPoller(Integer.parseInt(id), jamaStatus);
            changeStreamPoller.setInterval(Integer.parseInt(env.getProperty("pollIntervalInMinutes")));
            changeStreamPoller.setJi(jamaI);
            changeStreamPoller.setJamaUpdateTracingInstrumentation(jamaUTI);
            scheduler.registerAndStartTask(changeStreamPoller);
        }
        return scheduler;
    }
	
    private JamaInstance configOnlineJamaInstance() {
        JamaConfig jamaConf = new JamaConfig();
        jamaConf.setJson(new CachingJsonHandler(jamaCache));
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

}
