package impactassessment.passiveprocessengine.instance;

import impactassessment.passiveprocessengine.definition.Artifact;
import impactassessment.passiveprocessengine.definition.ArtifactType;
import org.neo4j.ogm.annotation.*;

@RelationshipEntity
public abstract class ArtifactIO implements java.io.Serializable{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    String id;
//		@GeneratedValue
//		private Long id; //not used outside of OGM neo4j

    @EndNode
    Artifact artifact;
    @Property
    String role;
    @StartNode
    WorkflowTask container;

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    ArtifactType artifactType;

    protected void setContainer(WorkflowTask wt) {
        this.container = wt;
        //FIXME: brittle setting/overriding of id
        this.id = role+"#"+artifact.getId()+"#"+wt.getId();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String outputRole) {
        this.role = outputRole;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public ArtifactIO(Artifact artifact) {
        super();
        this.artifact = artifact;
    }

    @Deprecated
    public ArtifactIO() {}

    public ArtifactIO(Artifact artifact, String role) {
        super();
        this.id = role+"#"+artifact.getId();
        this.artifact = artifact;
        this.role = role;
    }

    public ArtifactIO(Artifact artifact, String role, ArtifactType artifactType) {
        super();
        this.id = role+"#"+artifact.getId();
        this.artifact = artifact;
        this.role = role;
        this.artifactType = artifactType;
    }


    @Override
    public String toString() {
        if (artifact.isRemovedAtOrigin())
            return "[DEL "+ artifact.getId() +"::"+ artifact.getType() + "]";
        else
            return "["+ artifact.getId() +"::"+ artifact.getType() + "]";
    }

}
