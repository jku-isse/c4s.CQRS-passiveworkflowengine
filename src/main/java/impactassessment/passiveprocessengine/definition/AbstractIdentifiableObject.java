package impactassessment.passiveprocessengine.definition;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import impactassessment.passiveprocessengine.instance.CorrelationTuple;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public abstract class AbstractIdentifiableObject implements IdentifiableObject {

	@Id
	protected String id;
	protected transient CorrelationTuple lastChangeDueTo;
	
	@Override
	public String getId() {
		return id;
	}
	
	
	
	@Override
	public Optional<CorrelationTuple> getLastChangeDueTo() {
		return Optional.ofNullable(lastChangeDueTo);
	}



	@Override
	public void setLastChangeDueTo(CorrelationTuple lastChangeDueTo) {
		this.lastChangeDueTo = lastChangeDueTo;
	}



	public AbstractIdentifiableObject(String id) {
		if (id == null || id.length() == 0) {
			this.id = UUID.randomUUID().toString();
		} else
			this.id = id;
	}



	@Deprecated
	public AbstractIdentifiableObject() {	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AbstractIdentifiableObject)) return false;
		AbstractIdentifiableObject that = (AbstractIdentifiableObject) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
