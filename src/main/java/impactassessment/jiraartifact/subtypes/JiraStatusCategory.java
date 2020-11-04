package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.StatusCategory;
import impactassessment.jiraartifact.subinterfaces.IJiraStatusCategory;

import java.net.URI;

public class JiraStatusCategory implements IJiraStatusCategory {

    private StatusCategory statusCategory;

    public JiraStatusCategory(StatusCategory statusCategory) {
        this.statusCategory = statusCategory;
    }

    @Override
    public Long getId() {
        return statusCategory.getId();
    }

    @Override
    public URI getSelf() {
        return statusCategory.getSelf();
    }

    @Override
    public String getKey() {
        return statusCategory.getKey();
    }

    @Override
    public String getName() {
        return statusCategory.getName();
    }
}
