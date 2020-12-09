package impactassessment.artifactconnector.jira.subinterfaces;

import java.net.URI;

public interface IJiraStatusCategory {

    Long getId();

    URI getSelf();

    String getKey();

    String getName();

}
