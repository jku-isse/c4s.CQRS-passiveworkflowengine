package impactassessment.passiveprocessengine.persistance;

import impactassessment.passiveprocessengine.definition.DecisionNodeDefinition;
import impactassessment.passiveprocessengine.definition.DefaultBranchDefinition;

@SuppressWarnings("deprecation")
public class PersistableBranchDefinition extends DefaultBranchDefinition {

    public void setDecisionNodeDefinition(DecisionNodeDefinition dnd) {
        this.dnd = dnd;
    }
}
