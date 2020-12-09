package impactassessment.artifactconnector.jama.subinterfaces;

import java.util.Date;

public interface IJamaProjectArtifact {
    String getProjectKey();

    String getName();

    String getDescription();

    boolean isFolder();

    IJamaUserArtifact getCreatedBy();

    Date getCreatedDate();

    Date getModifiedDate();

    IJamaUserArtifact getModifiedBy();

    @Override
    String toString();
}
