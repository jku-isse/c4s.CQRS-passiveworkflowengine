package impactassessment.featureflags;


import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Conditional;

public class ConditionNone extends NoneNestedConditions {

    public ConditionNone() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @Conditional(ConditionJiraLive.class)
    static class JiraEnabled {}

    @Conditional(ConditionJiraDemo.class)
    static class JiraDemoEnabled {}

    @Conditional(ConditionJama.class)
    static class JamaEnabled {}

}
