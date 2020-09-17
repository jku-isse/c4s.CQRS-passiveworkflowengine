package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.CorrelationTuple;

import java.util.Optional;

public interface IdentifiableObject {

    String getId();

    Optional<CorrelationTuple> getLastChangeDueTo();

    void setLastChangeDueTo(CorrelationTuple lastChangeDueTo);

}
