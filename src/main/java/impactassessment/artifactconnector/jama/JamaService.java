package impactassessment.artifactconnector.jama;

import impactassessment.artifactconnector.ArtifactIdentifier;
import impactassessment.artifactconnector.IArtifact;
import impactassessment.artifactconnector.IArtifactService;

public class JamaService implements IArtifactService {

    private static final String TYPE = IJamaArtifact.class.getSimpleName();

    @Override
    public boolean provides(String type) {
        return type.equals(TYPE);
    }

    @Override
    public IArtifact get(ArtifactIdentifier id, String workflowId) {
        return null;
    }
}
