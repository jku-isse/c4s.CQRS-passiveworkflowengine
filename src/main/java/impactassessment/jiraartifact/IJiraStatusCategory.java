package impactassessment.jiraartifact;

import java.net.URI;

public interface IJiraStatusCategory {

    Long getId();

    URI getSelf();

    String getKey();

    String getName();

}
