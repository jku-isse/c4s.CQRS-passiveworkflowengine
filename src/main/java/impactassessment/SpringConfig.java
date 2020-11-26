package impactassessment;

import c4s.jiralightconnector.*;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.JiraChangeSubscriber;
import impactassessment.jiraartifact.JiraService;
import impactassessment.registry.IRegisterService;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import passiveprocessengine.definition.WorkflowDefinition;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

@Configuration
public class SpringConfig {

//    @Bean
//    public IJiraArtifactService getJiraArtifactService(JiraChangeSubscriber jiraChangeSubscriber) {
//        // uses JSON image of Jira data in resources folder
//        return new JiraJsonService(jiraChangeSubscriber);
//    }

    @Bean
    public IJiraArtifactService getJiraArtifactService(JiraInstance jiraInstance, JiraChangeSubscriber jiraChangeSubscriber) {
        // connects directly to a Jira server
        return new JiraService(jiraInstance, jiraChangeSubscriber);
    }

    @Bean
    @Autowired
    public IRegisterService getIRegisterService(WorkflowDefinitionRegistry registry) {
        return new LocalRegisterService(registry);
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
        Properties props = new Properties();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("application.properties").getFile());
            FileReader reader = new FileReader(file);
            props.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String uri =  props.getProperty("jiraServerURI");
        String username =  props.getProperty("jiraConnectorUsername");
        String pw =  props.getProperty("jiraConnectorPassword");
        return (new AsynchronousJiraRestClientFactory()).createWithBasicHttpAuthentication(URI.create(uri), username, pw);
    }
}
