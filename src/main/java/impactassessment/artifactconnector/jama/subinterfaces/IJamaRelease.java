package impactassessment.artifactconnector.jama.subinterfaces;

import java.util.Date;

public interface IJamaRelease {
    String getName();

    String getDescription();

    IJamaProjectArtifact getProject();

    Date getReleaseDate();

    boolean isActive();

    boolean isArchived();

    Integer getItemCount();

    @Override
    String toString();
}
