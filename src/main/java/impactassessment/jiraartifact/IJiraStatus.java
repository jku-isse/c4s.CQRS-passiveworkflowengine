package impactassessment.jiraartifact;

import java.net.URI;

public interface IJiraStatus {

    URI getSelf();

    String getName();

    Long getId();

    String getDescription();

    IJiraStatusCategory getStatusCategory();

}
