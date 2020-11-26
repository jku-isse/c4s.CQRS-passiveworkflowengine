package impactassessment.artifactconnector.jira.subinterfaces;

import java.net.URI;

public interface IJiraUser {

    URI getSelf();

    String getName();

    String getDisplayName();

    String getAccoutId();

    String getEmailAddress();

    boolean isActive();

}
