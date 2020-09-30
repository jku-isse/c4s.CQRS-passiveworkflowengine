package impactassessment.passiveprocessengine.instance;

import impactassessment.passiveprocessengine.definition.Artifact;
import impactassessment.passiveprocessengine.definition.ArtifactType;
import org.neo4j.ogm.annotation.RelationshipEntity;

@RelationshipEntity(type="TASK_IO")
public class ArtifactOutput extends ArtifactIO {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ArtifactOutput(Artifact artifact, String role) {
        super(artifact, role);
    }

    public ArtifactOutput(Artifact artifact) {
        super(artifact);
    }

    public ArtifactOutput(ArtifactInput ai) {
        super(ai.artifact, ai.role, ai.artifactType);
        id = ai.id;
        container = ai.container;
    }

    public ArtifactOutput(Artifact artifact, String role, ArtifactType artifactType) {
        super(artifact, role, artifactType);
    }

    @Deprecated
    public ArtifactOutput() {
        super();
    }
}
