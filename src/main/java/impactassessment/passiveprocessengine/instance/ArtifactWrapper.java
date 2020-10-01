package impactassessment.passiveprocessengine.instance;

import impactassessment.passiveprocessengine.definition.AbstractArtifact;
import impactassessment.passiveprocessengine.definition.Artifact;
import impactassessment.passiveprocessengine.definition.ArtifactType;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class ArtifactWrapper extends AbstractArtifact {

	private static final long serialVersionUID = 1L;
	private Object wrappedArtifact;
	
	
	@Deprecated
	public ArtifactWrapper() {
		super();
	}
	
	public ArtifactWrapper(String id, String type, WorkflowInstance wfi, Object wrappedArtifact) {
		super(id, new ArtifactType(type), wfi);
		this.wrappedArtifact = wrappedArtifact;
	}

	@Override
	public String getId() {
		return id;
	}

	public Object getWrappedArtifact() {
		return wrappedArtifact;
	}
	
	@Override
	public Artifact getParentArtifact() {
		return null;
	}

	@Override
	public String toString() {
		String wa = wrappedArtifact != null ? wrappedArtifact.getClass().getSimpleName() : "NONE";
		return "ArtifactWrapper [type=" + type + ", id=" + id + ", wrappedArtifact=" + wa + ", wfi="+ getWorkflowId() +"]";
	}
	
	public void updateWrappedArtifact(Object newWrappedArtifact) {
		this.wrappedArtifact = newWrappedArtifact;
	}
	
}
