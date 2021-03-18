package impactassessment.artifactconnector.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaUserArtifact;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class JamaDemoService implements IJamaService {
    @Override
    public Optional<IJamaArtifact> get(Integer id, String workflow) {
        log.warn("not implemented in JamaDemoService!");
        return Optional.empty();
    }

    @Override
    public Optional<IJamaArtifact> get(Integer id) {
        log.warn("not implemented in JamaDemoService!");
        return Optional.empty();
    }

    @Override
    public IJamaArtifact convert(JamaItem item) {
        log.warn("not implemented in JamaDemoService!");
        return null;
    }

    @Override
    public IJamaProjectArtifact convertProject(JamaProject proj) {
        log.warn("not implemented in JamaDemoService!");
        return null;
    }

    @Override
    public IJamaUserArtifact convertUser(JamaUser user) {
        log.warn("not implemented in JamaDemoService!");
        return null;
    }

    @Override
    public String getJamaServerUrl(JamaItem jamaItem) {
        log.warn("not implemented in JamaDemoService!");
        return null;
    }

    @Override
    public boolean provides(String s) {
        log.warn("not implemented in JamaDemoService!");
        return false;
    }

    @Override
    public Optional<IArtifact> get(ArtifactIdentifier artifactIdentifier, String s) {
        log.warn("not implemented in JamaDemoService!");
        return Optional.empty();
    }

    @Override
    public void injectArtifactService(IArtifact iArtifact, String s) {
        log.warn("not implemented in JamaDemoService!");
    }

    @Override
    public void deleteDataScope(String s) {
        log.warn("not implemented in JamaDemoService!");
    }
}
