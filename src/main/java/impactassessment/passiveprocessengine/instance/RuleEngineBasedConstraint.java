package impactassessment.passiveprocessengine.instance;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import impactassessment.passiveprocessengine.instance.QACheckDocument.QAConstraint;

@NodeEntity
public class RuleEngineBasedConstraint extends QAConstraint {

	@Deprecated
	public RuleEngineBasedConstraint() { super();
	}
	
	public RuleEngineBasedConstraint(String id, QACheckDocument parent, String constraintType, WorkflowInstance wfi, String description) {
		super(id, parent, wfi);
		if (id == null && constraintType != null && wfi != null && wfi.getId() != null) { // then super has set a random uuid one, we override this here for predictable ids if constrainttype and wfi are not null
			this.id = wfi.getId()+"-"+constraintType;
		}
		this.constraintType = constraintType;
		super.description = description;
	}
	
	@Property
	private String constraintType;
	
	public String getConstraintType() {
		return constraintType;
	}
	
	
	@Override
	public void checkConstraint() {
		// no op, check is triggered/run by rule engine, only output stored here
	}

	public boolean isAffectedBy(ConstraintTrigger ct) {
		if (ct.getConstraintsToTrigger().contains("*") || ct.doesConstraintTypeMatchConstraintsToTrigger(getConstraintType()))
			return true;
		return false;
	}

	public void setEvaluated(CorrelationTuple lastChangeDueTo) {
		setEvaluationStatusMessage("");
		setEvaluationStatus(EvaluationState.SUCCESS);
		setLastChangeDueTo(lastChangeDueTo);
	}

	@Override
	public String toString() {
		return "RuleEngineBasedConstraint [constraintType=" + constraintType + " " + super.toString() + " ]";
	}

	
	
}
