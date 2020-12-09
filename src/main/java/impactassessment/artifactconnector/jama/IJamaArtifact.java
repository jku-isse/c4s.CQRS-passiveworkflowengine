package impactassessment.artifactconnector.jama;

import com.jamasoftware.services.restclient.exception.RestClientException;
import impactassessment.artifactconnector.IArtifact;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaProjectArtifact;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaRelease;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaUserArtifact;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IJamaArtifact extends IArtifact {
    Integer getId();

    boolean isProject();

    String getName();

    String getGlobalId();

    IJamaProjectArtifact getProject();

    String getItemType();

    IJamaUserArtifact getCreatedBy();

    IJamaUserArtifact getModifiedBy();

    String getDocumentKey();

    Date getCreatedDate();

    Date getModifiedDate();

    Date getLastActivityDate();

    List<IJamaArtifact> getChildren(String workflowId);

    List<IJamaArtifact> prefetchDownstreamItems(String workflowId);

    List<IJamaArtifact> getDownstreamItems(String workflowId);

    String getStringValue(String fieldName);

    Date getDateValue(String fieldName);

    Boolean getBooleanValue(String fieldName);

    Integer getIntegerValue(String fieldName);

    IJamaProjectArtifact getJamaProjectValue(String fieldName);

    IJamaRelease getJamaReleaseValue(String fieldName);

    IJamaUserArtifact getJamaUserValue(String fieldName);
}
