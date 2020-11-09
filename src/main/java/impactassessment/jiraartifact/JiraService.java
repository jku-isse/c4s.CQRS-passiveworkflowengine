package impactassessment.jiraartifact;

import c4s.jiralightconnector.*;
import com.atlassian.jira.rest.client.api.domain.Issue;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Slf4j
public class JiraService implements IJiraArtifactService {

    private JiraInstance jira;
    private JiraChangeSubscriber jiraChangeSubscriber;


    public JiraService(IssueCache issueCache, JiraChangeSubscriber changeSubscriber, MonitoringState monitoringState) {
        jira = new JiraInstance(issueCache, changeSubscriber, monitoringState);
        jiraChangeSubscriber = changeSubscriber;

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

        jira.init(username, pw, uri);
    }

    @Override
    public IJiraArtifact get(String artifactKey, String workflowId) {
        IssueAgent issueAgent = jira.fetchAndMonitor(artifactKey);
        if (issueAgent == null) {
            log.debug("Not able to fetch Jira Issue");
            return null;
        } else  {
            log.debug("Successfully fetched Jira Issue");
            jiraChangeSubscriber.addUsage(workflowId, artifactKey);
            return new JiraArtifact(issueAgent.getIssue());
        }
    }

}
