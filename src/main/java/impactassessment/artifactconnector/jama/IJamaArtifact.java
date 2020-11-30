package impactassessment.artifactconnector.jama;

import impactassessment.artifactconnector.jama.subtypes.JamaProjectArtifact;

import java.util.Date;
import java.util.Optional;

public interface IJamaArtifact {

    Optional<String> getStringValue(String fieldName);
    Optional<Date> getDateValue(String fieldName);
    Optional<Boolean> getBooleanValue(String fieldName);
    Optional<Integer> getIntegerValue(String fieldName);
    Optional<JamaProjectArtifact> getJamaProject(String fieldName);
    
}
