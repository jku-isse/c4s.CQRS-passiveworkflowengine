package counter.workflowmodel;

import java.util.Optional;
import java.util.UUID;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import counter.analytics.CorrelationTuple;

@NodeEntity
public abstract class IdentifiableObject {

	@Id
	protected String id;
	protected transient CorrelationTuple lastChangeDueTo; 
	
	public String getId() {
		return id;
	}
	
	
	
	public Optional<CorrelationTuple> getLastChangeDueTo() {
		return Optional.ofNullable(lastChangeDueTo);
	}



	public void setLastChangeDueTo(CorrelationTuple lastChangeDueTo) {
		this.lastChangeDueTo = lastChangeDueTo;
	}



	public IdentifiableObject(String id) {
		if (id == null || id.length() == 0) {
			this.id = UUID.randomUUID().toString();
		} else
			this.id = id;
	}



	@Deprecated
	public IdentifiableObject() {	}
}
