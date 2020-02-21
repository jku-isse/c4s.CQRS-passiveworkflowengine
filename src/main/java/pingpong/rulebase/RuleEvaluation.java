package pingpong.rulebase;

import org.kie.api.runtime.KieSession;

public class RuleEvaluation {

    private KieSession kieSession;

    public RuleEvaluation() {
        kieSession = new RuleBaseFactory().getKieSession();
    }

    public void insertAndFire() {
        kieSession.insert(new Count());
        kieSession.fireAllRules();
    }
}
