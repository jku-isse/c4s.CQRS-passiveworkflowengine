package impactassessment;

import c4s.jamaconnector.IJamaChangeSubscriber;
import c4s.jamaconnector.OfflineHttpClientMock;
import c4s.jamaconnector.cache.CachedResourcePool;
import c4s.jamaconnector.cache.CachingJsonHandler;
import c4s.jamaconnector.cache.CouchDBJamaCache;
import c4s.jiralightconnector.*;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.IArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.registry.IRegisterService;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

@Configuration
public class SpringConfig {

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
        return new ChangeStreamPoller(2);
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
    public JamaService getJamaService(JamaInstance jamaInstance, IJamaChangeSubscriber jamaChangeSubscriber) {
        return new JamaService(jamaInstance, jamaChangeSubscriber);
    }

    @Bean
    public JamaInstance getJamaInstance(CouchDbClient dbClient) {
        JamaConfig jamaConf = new JamaConfig();
        CouchDBJamaCache cache = new CouchDBJamaCache(dbClient);
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
                .setDbName(props.getProperty("jiraCacheCouchDBname", "jamaitems2"))
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
