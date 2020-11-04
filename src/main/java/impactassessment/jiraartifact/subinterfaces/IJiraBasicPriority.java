package impactassessment.jiraartifact.subinterfaces;

import java.net.URI;

public interface IJiraBasicPriority {
    URI getSelf();
    String getName();
    Long getId();
}
