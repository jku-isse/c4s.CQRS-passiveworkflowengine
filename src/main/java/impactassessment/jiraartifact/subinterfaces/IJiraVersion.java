package impactassessment.jiraartifact.subinterfaces;

import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.net.URI;

public interface IJiraVersion {

    URI getSelf();

    @Nullable
    Long getId();

    String getDescription();

    String getName();

    boolean isArchived();

    boolean isReleased();

    @Nullable
    DateTime getReleaseDate();

}
