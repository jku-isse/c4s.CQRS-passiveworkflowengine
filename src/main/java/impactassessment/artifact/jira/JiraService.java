package impactassessment.artifact.jira;

import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jiralightconnector.*;
import impactassessment.artifact.base.IArtifact;
import impactassessment.artifact.base.IArtifactService;
import impactassessment.artifact.base.IIssueLink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Component
public class JiraService implements IArtifactService {

    private JiraInstance jira;

    public JiraService() {
        jira = new JiraInstance(new MockCache(), new MockSubscriber(), new MockMonitoring());

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
    public IArtifact get(String key) {
        IssueAgent issueAgent = jira.fetchAndMonitor(key);
        log.debug(issueAgent.toString());
        return new JiraArtifact(issueAgent.getIssue());
    }

    public static class MockCache implements IssueCache {

        @Override
        public Optional<IssueAgent> getFromCache(String s) {
            return Optional.empty();
        }

        @Override
        public void insertOrUpdate(IssueAgent issueAgent) {

        }

        @Override
        public void updateIfExists(IssueAgent issueAgent) {

        }

        @Override
        public Optional<ZonedDateTime> getLastRefreshedOn() {
            return Optional.empty();
        }

        @Override
        public void setLastRefreshedOn(ZonedDateTime zonedDateTime) {

        }

        @Override
        public void removeFromCache(String s) {

        }
    }
    public static class MockSubscriber implements ChangeSubscriber {

        @Override
        public void handleUpdatedIssues(List<IssueAgent> list, CorrelationTuple correlationTuple) {

        }
    }
    public static class MockMonitoring implements MonitoringState {

        @Override
        public Set<String> getMonitoredIssueKeys() {
            return null;
        }

        @Override
        public void removeMonitoredIssueKey(String s) {

        }

        @Override
        public void addMonitoredIssueKey(String s) {

        }
    }
}
