package counter.workflowmodel.definition;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import counter.workflowmodel.*;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class QACheckDocument extends AbstractArtifact {

	@Override
	public String toString() {
		return "QACheckDocument [id=" + id + ", constraints=" + constraints + "]";
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final ArtifactType artType = new ArtifactType(QACheckDocument.class.getSimpleName());
	
	@Deprecated
	public QACheckDocument() {
		super();
	}
	
	public QACheckDocument(String id, WorkflowInstance wfi) {
		super(id, artType, wfi);		
	}
	
	@Override
	public Artifact getParentArtifact() {		
		return null;
	}

	@Relationship(type="HAS_CONSTRAINTS", direction=Relationship.OUTGOING)
	List<QAConstraint> constraints = new ArrayList<QAConstraint>();
	
	public boolean areAllConstraintsFulfilled() {
		// return false if any constraint is not fulfilled
		// returns true if no constraints inserted
		return !constraints.stream()
				.anyMatch(c -> !c.isFulfilled());								
	}
	
	public void addConstraint(QAConstraint qac) {
		if (!constraints.contains(qac)) {
			constraints.add(qac);
		}
	}
	
	public void checkAll() {
		constraints.stream().forEach(c -> c.checkConstraint());
	}
	
	public List<QAConstraint> getConstraintsReadonly() {
		return Collections.unmodifiableList(constraints);
	}
	
	@NodeEntity("QAConstraint")
	public abstract static class QAConstraint extends AbstractArtifact {

		

		@Override
		public String toString() {
			return "QAConstraint [id=" + id + ", fulfilledFor=" + fulfilledFor + ", unsatisfiedFor=" + unsatisfiedFor
					+ ", evaluationStatus=" + evaluationStatus + ", evaluationStatusMessage=" + evaluationStatusMessage
					+ ", lastEvaluated=" + lastEvaluated + ", lastChanged=" + lastChanged + "]";
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public static final ArtifactType constraintType = new ArtifactType(QAConstraint.class.getSimpleName());
		//public static final String SUCCESS_STATUS = "SUCCESS";
		
		@Relationship(type="HAS_CONSTRAINTS", direction=Relationship.INCOMING)
		QACheckDocument parent;
		@Property
		String description;
		@Relationship(type="FULFILLED_CONSTRAINT")
		Set<ResourceLink> fulfilledFor = new HashSet<ResourceLink>();
		@Relationship(type="FAILED_CONSTRAINT")
		Set<ResourceLink> unsatisfiedFor = new HashSet<ResourceLink>();						
		@Property
		EvaluationState evaluationStatus = EvaluationState.NOT_YET_EVALUATED;
		@Property
		String evaluationStatusMessage = "";
		@Property
		Instant lastEvaluated = Instant.MIN;
		@Property
		Instant lastChanged = Instant.MIN;		
		@Property
		int orderInParentDoc = 0;
		
		transient boolean wasConstraintChecked = false;
		
		
		private transient  Set<ResourceLink> checkedEntities = new HashSet<ResourceLink>();
		
		abstract public void checkConstraint();

		@Deprecated
		public QAConstraint() {
			super();
		}
		
		public QAConstraint(String id, QACheckDocument parent, WorkflowInstance wfi, int orderInDoc) {
			super(id, constraintType, wfi);
			this.orderInParentDoc = orderInDoc;
			this.parent = parent;
			if (parent != null) {
				parent.addConstraint(this);
			}
		}
		
		public QAConstraint(String id, QACheckDocument parent, WorkflowInstance wfi) {
			super(id, constraintType, wfi);
			this.parent = parent;
			if (parent != null) {
				parent.addConstraint(this);
				this.orderInParentDoc = parent.constraints.size();
			}
		}
		
		public boolean isFulfilled() {
			return evaluationStatus.equals(EvaluationState.SUCCESS) && unsatisfiedFor.isEmpty();
		}
		
		@Override
		public Artifact getParentArtifact() {
			return parent;
		}				
		
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Set<ResourceLink> getFulfilledForReadOnly() {
			return Collections.unmodifiableSet(fulfilledFor);
		}

		public Set<ResourceLink> getUnsatisfiedForReadOnly() {
			return Collections.unmodifiableSet(unsatisfiedFor);
		}

		public EvaluationState getEvaluationStatus() {
			return evaluationStatus;
		}

		public void setEvaluationStatus(EvaluationState evaluationStatus) {
			this.evaluationStatus = evaluationStatus;
		}				
		
		public String getEvaluationStatusMessage() {
			return evaluationStatusMessage;
		}

		public void setEvaluationStatusMessage(String evaluationStatusMessage) {
			this.evaluationStatusMessage = evaluationStatusMessage;
		}

		public Instant getLastEvaluated() {
			return lastEvaluated;
		}

		public void setLastEvaluated(Instant lastEvaluated) {
			this.lastEvaluated = lastEvaluated;
		}

		public Instant getLastChanged() {
			return lastChanged;
		}

		public void setLastChanged(Instant lastChanged) {
			this.lastChanged = lastChanged;
		}

		public boolean addAs(boolean satisfied, ResourceLink... links) {
			wasConstraintChecked = true;
			boolean dirty = false;
			Instant timestamp = Instant.now();
			Set<ResourceLink> addTo, removeFrom;
			if (satisfied) {
				addTo = this.fulfilledFor;
				removeFrom = this.unsatisfiedFor;
			} else {
				removeFrom = this.fulfilledFor;
				addTo = this.unsatisfiedFor;
			}
			for (ResourceLink rl : links) {				
				checkedEntities.add(rl);
				if (removeFrom.remove(rl) | addTo.add(rl)) { // we must evaluate both sides!!
					dirty = true;
				}
			}
			if (dirty) {
				this.lastChanged = timestamp;
			}
			this.lastEvaluated = timestamp; 
			return dirty;
		}

		public boolean addAs(boolean satisfied, List<ResourceLink> links) {
			return addAs(satisfied, links.stream().toArray(ResourceLink[]::new));
		}

		public void removeAllResourceLinks() {
			this.fulfilledFor.clear();
			this.unsatisfiedFor.clear();
		}
		
		public boolean removeUnsatisfied(ResourceLink rl) {
			return this.unsatisfiedFor.remove(rl);
		}
		
		public void signalCheckingComplete() {
			if (wasConstraintChecked) {
				fulfilledFor.retainAll(checkedEntities);
				unsatisfiedFor.retainAll(checkedEntities);
				checkedEntities.clear();
				wasConstraintChecked = false;
			}
		}
		
		public static enum EvaluationState {
			NOT_YET_EVALUATED,
			SUCCESS,
			FAILURE
		}
		
	}
	
}
