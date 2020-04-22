package impactassessment.rulebase;

public class RuleBaseUtils {
    public static boolean compareWorkflows(String ct, String qa) {
        return qa.equals(ct);
    }
    public static boolean checkConstraintType(String qa) {
        return qa.equals("CheckSWRequirementReleased");
    }
}
