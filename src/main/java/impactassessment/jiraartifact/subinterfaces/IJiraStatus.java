package impactassessment.jiraartifact.subinterfaces;

import java.net.URI;

public interface IJiraStatus {

    URI getSelf();

    String getName();

    Long getId();

    String getDescription();

    IJiraStatusCategory getStatusCategory();

}
