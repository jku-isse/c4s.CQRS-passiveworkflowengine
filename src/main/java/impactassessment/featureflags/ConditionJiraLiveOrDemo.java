package impactassessment.featureflags;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.context.annotation.Conditional;

public class ConditionJiraLiveOrDemo extends AnyNestedCondition {

    public ConditionJiraLiveOrDemo() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @Conditional(ConditionJiraLive.class)
    static class JiraEnabled {}

    @Conditional(ConditionJiraDemo.class)
    static class JiraDemoEnabled {}
}
