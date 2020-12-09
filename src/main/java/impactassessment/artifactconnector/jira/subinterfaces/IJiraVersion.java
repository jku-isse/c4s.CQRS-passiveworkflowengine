package impactassessment.artifactconnector.jira.subinterfaces;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Date;

public interface IJiraVersion {

    URI getSelf();

    @Nullable
    Long getId();

    String getDescription();

    String getName();

    boolean isArchived();

    boolean isReleased();

    @Nullable
    Date getReleaseDate();

}
