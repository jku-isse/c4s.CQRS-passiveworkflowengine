package impactassessment.passiveprocessengine.definition;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import impactassessment.passiveprocessengine.instance.CorrelationTuple;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof IdentifiableObject)) return false;
		IdentifiableObject that = (IdentifiableObject) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
