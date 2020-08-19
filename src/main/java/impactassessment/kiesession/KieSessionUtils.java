package impactassessment.kiesession;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KieSessionUtils {
    public static boolean compareWorkflows(String ct, String qa) {
        return qa.equals(ct);
    }
    public static boolean checkConstraintType(String qa) {
        return qa.equals("CheckSWRequirementReleased");
    }
}
