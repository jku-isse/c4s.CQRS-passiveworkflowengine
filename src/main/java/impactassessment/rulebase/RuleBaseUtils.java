package impactassessment.rulebase;

import impactassessment.workflowmodel.definition.ConstraintTrigger;
import impactassessment.workflowmodel.definition.QACheckDocument;
import impactassessment.workflowmodel.definition.RuleEngineBasedConstraint;

public class RuleBaseUtils {
    public static boolean compareWorkflows(String ct, String qa) {
        return qa.equals(ct);
    }
    public static boolean checkConstraintType(String qa) {
        return qa.equals("CheckSWRequirementReleased");
    }
}
