package impactassessment.passiveprocessengine.instance;

import impactassessment.passiveprocessengine.definition.Artifact;
import impactassessment.passiveprocessengine.definition.ArtifactType;
import org.neo4j.ogm.annotation.RelationshipEntity;

@RelationshipEntity(type="TASK_IO")
public class ArtifactInput extends ArtifactIO {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    public ArtifactInput(Artifact artifact, String role) {
        super(artifact, role);
    }

    public ArtifactInput(Artifact artifact) {
        super(artifact);
    }

    public ArtifactInput(ArtifactOutput ao) {
        id = ao.id;
        role = ao.role;
        container = ao.container;
        artifact = ao.artifact;
    }

    public ArtifactInput(Artifact artifact, String role, ArtifactType artifactType) {
        super(artifact, role, artifactType);
    }

    @Deprecated
    public ArtifactInput(){
        super();
    }
}
