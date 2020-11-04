package impactassessment.jiraartifact.subinterfaces;

import java.net.URI;

public interface IJiraStatusCategory {

    Long getId();

    URI getSelf();

    String getKey();

    String getName();

}
