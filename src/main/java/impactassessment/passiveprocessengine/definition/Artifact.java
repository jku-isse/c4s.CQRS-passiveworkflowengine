package impactassessment.passiveprocessengine.definition;

public interface Artifact extends java.io.Serializable{
	public String getId();
	public ArtifactType getType();
	public Artifact getParentArtifact();
	public void setRemovedAtOriginFlag();
	public boolean isRemovedAtOrigin();
	public String getWorkflowId();
	//public void setWorkflowId(String workflowInstanceId);
}
