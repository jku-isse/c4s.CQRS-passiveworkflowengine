package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import impactassessment.jiraartifact.subinterfaces.IJiraBasicPriority;

import java.net.URI;

public class JiraBasicPriority implements IJiraBasicPriority {

    private BasicPriority basicPriority;

    public JiraBasicPriority(BasicPriority basicPriority) {
        this.basicPriority = basicPriority;
    }

    @Override
    public URI getSelf() {
        return basicPriority.getSelf();
    }

    @Override
    public String getName() {
        return basicPriority.getName();
    }

    @Override
    public Long getId() {
        return basicPriority.getId();
    }

    @Override
    public String toString() {
        return "JiraBasicPriority{" +
                "basicPriority=" + basicPriority +
                '}';
    }
}
