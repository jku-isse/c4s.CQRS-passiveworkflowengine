package impactassessment.passiveprocessengine.instance;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class TypedArtifactWrapper<T> extends ArtifactWrapper{

	private static final long serialVersionUID = 1L;
	private transient T typedWrappedArtifact;
	
	@Deprecated
	public TypedArtifactWrapper() {
		super();
	}
	
	public TypedArtifactWrapper(String id, String type, T wrappedArtifact, WorkflowInstance wfi) {
		super(id, type, wfi, wrappedArtifact);
		this.typedWrappedArtifact = wrappedArtifact;
	}

	public T getArtifact() {
		return typedWrappedArtifact;
	}
	
}
