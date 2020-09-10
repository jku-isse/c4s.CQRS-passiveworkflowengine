package impactassessment.passiveprocessengine.definition;

public abstract class AbstractWorkflowObject {
	
	protected String id;
	
	public String getId() {
		return id;
	}
	
	public AbstractWorkflowObject(String id) {
		this.id = id;
	}
}
