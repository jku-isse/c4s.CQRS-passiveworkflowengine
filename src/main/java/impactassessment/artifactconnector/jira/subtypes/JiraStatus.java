package impactassessment.artifactconnector.jira.subtypes;

import artifactapi.jira.subtypes.IJiraStatus;
import artifactapi.jira.subtypes.IJiraStatusCategory;
import com.atlassian.jira.rest.client.api.domain.Status;

import java.net.URI;

public class JiraStatus implements IJiraStatus {

    private Status status;
    private IJiraStatusCategory jiraStatusCategory;

    public JiraStatus(Status status) {
        this.status = status;
        this.jiraStatusCategory = new JiraStatusCategory(status.getStatusCategory());
    }

    @Override
    public URI getSelf() {
        return status.getSelf();
    }

    @Override
    public String getName() {
        return status.getName();
    }

    @Override
    public Long getId() {
        return status.getId();
    }

    @Override
    public String getDescription() {
        return status.getDescription();
    }

    @Override
    public IJiraStatusCategory getStatusCategory() {
        return jiraStatusCategory;
    }

    @Override
    public String toString() {
        return "JiraStatus{" +
                "status=" + status +
                ", jiraStatusCategory=" + jiraStatusCategory +
                '}';
    }
}
