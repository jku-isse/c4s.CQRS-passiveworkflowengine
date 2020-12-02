package impactassessment.artifactconnector.jama;

import com.jamasoftware.services.restclient.exception.RestClientException;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaProjectArtifact;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaRelease;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaUserArtifact;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IJamaArtifact {
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

    List<IJamaArtifact> getChildren() throws RestClientException;

    List<IJamaArtifact> prefetchDownstreamItems() throws RestClientException;

    List<IJamaArtifact> getDownstreamItems();

    Optional<String> getStringValue(String fieldName);

    Optional<Date> getDateValue(String fieldName);

    Optional<Boolean> getBooleanValue(String fieldName);

    Optional<Integer> getIntegerValue(String fieldName);

    Optional<IJamaProjectArtifact> getJamaProjectValue(String fieldName);

    Optional<IJamaRelease> getJamaReleaseValue(String fieldName);

    Optional<IJamaUserArtifact> getJamaUserValue(String fieldName);
}
