package impactassessment.passiveprocessengine.instance;

import java.util.HashSet;
import java.util.Set;

import impactassessment.passiveprocessengine.definition.AbstractArtifact;
import impactassessment.passiveprocessengine.definition.Artifact;
import impactassessment.passiveprocessengine.definition.ArtifactType;

public class ConstraintTrigger extends AbstractArtifact {

	public static ArtifactType at = new ArtifactType(ConstraintTrigger.class.getSimpleName());
	//private List<String> workflowTaskIds = new ArrayList<>();
	//private String workflowTaskId = null;
	private Set<String> constraintsToTrigger = new HashSet<String>();
	private CorrelationTuple requestCorrelation;
	
	@Override
	public Artifact getParentArtifact() {
		return null;
	}

	@Deprecated
	public ConstraintTrigger() {}
	
	public ConstraintTrigger(WorkflowInstance wfi) {
		super(null, at, wfi);
	}
	
	public ConstraintTrigger(WorkflowInstance wfi, CorrelationTuple requestCorrelation) {
		super(null, at, wfi);
		this.requestCorrelation = requestCorrelation;
	}

	public Set<String> getConstraintsToTrigger() {
		return constraintsToTrigger;
	}

	public void setConstraintsToTrigger(Set<String> constraintsToTrigger) {
		this.constraintsToTrigger = constraintsToTrigger;
	}
	
	public void addConstraint(String constraintType) {
		this.constraintsToTrigger.add(constraintType);
	}
	
//	public void setWorkflowTaskId(String id) {
//		workflowTaskId = id;
//	}
//	
//	public List<String> getWftIds() {
//		return workflowTaskIds;
//	}
//	
//	public void setWftIds(List<String> l) {
//		workflowTaskIds = l;
//	}
	
	public boolean doesConstraintTypeMatchConstraintsToTrigger(String constraintType) {
		return this.constraintsToTrigger.stream()
			//.filter(c -> c.endsWith(constraintType))
			.filter(c -> c.equals(constraintType))
			.findAny()
			.isPresent();
	}

	public CorrelationTuple getRequestCorrelation() {
		return requestCorrelation;
	}

	public void setRequestCorrelation(CorrelationTuple requestCorrelation) {
		this.requestCorrelation = requestCorrelation;
	}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConstraintTrigger{");
        sb.append("constraintsToTrigger=[");
        constraintsToTrigger.stream().forEach(c -> sb.append(c).append(", "));
        sb.append("], requestCorrelation=").append(requestCorrelation);
        sb.append(", workflow id=").append(workflow.getId());
        sb.append(", id='").append(id).append('\'');
        sb.append('}');
        return sb.toString();
    }
}