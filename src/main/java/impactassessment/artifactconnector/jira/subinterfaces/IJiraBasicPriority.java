package impactassessment.artifactconnector.jira.subinterfaces;

import java.net.URI;

public interface IJiraBasicPriority {
    URI getSelf();
    String getName();
    Long getId();
}
