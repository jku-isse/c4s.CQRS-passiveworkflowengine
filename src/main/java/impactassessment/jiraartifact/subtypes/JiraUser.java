package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.domain.User;
import impactassessment.jiraartifact.subinterfaces.IJiraUser;

import java.net.URI;

public class JiraUser implements IJiraUser {

    private User user;

    public JiraUser(User user) {
        this.user = user;
    }

    @Override
    public URI getSelf() {
        return user.getSelf();
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public String getDisplayName() {
        return user.getDisplayName();
    }

    @Override
    public String getAccoutId() {
        return user.getAccountId();
    }

    @Override
    public String getEmailAddress() {
        return user.getEmailAddress();
    }

    @Override
    public boolean isActive() {
        return user.isActive();
    }
}
