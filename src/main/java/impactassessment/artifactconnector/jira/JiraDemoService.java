package impactassessment.artifactconnector.jira;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.jira.IJiraArtifact;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class JiraDemoService implements IJiraService {
    @Override
    public Optional<IJiraArtifact> getIssue(String id, String workflow) {
        log.warn("not implemented in JiraDemoService!");
        return Optional.empty();
    }

    @Override
    public Optional<IJiraArtifact> getIssue(String key) {
        log.warn("not implemented in JiraDemoService!");
        return Optional.empty();
    }

    @Override
    public boolean provides(String s) {
        log.warn("not implemented in JiraDemoService!");
        return false;
    }

    @Override
    public Optional<IArtifact> get(ArtifactIdentifier artifactIdentifier, String s) {
        log.warn("not implemented in JiraDemoService!");
        return Optional.empty();
    }

    @Override
    public void injectArtifactService(IArtifact iArtifact, String s) {
        log.warn("not implemented in JiraDemoService!");
    }

    @Override
    public void deleteDataScope(String s) {
        log.warn("not implemented in JiraDemoService!");
    }
}
