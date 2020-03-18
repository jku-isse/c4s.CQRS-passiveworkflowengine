package impactassessment.snapshots;

import impactassessment.api.CreatedWorkflowEvt;
import impactassessment.api.EnableCmd;
import impactassessment.api.EnabledEvt;
import impactassessment.command.WorkflowAggregate;
import impactassessment.rulebase.RuleBaseService;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = SpringTestConfig.class)
@ExtendWith(SpringExtension.class)
public class TestSnapshot {

    /**
     * Focus of a Test Fixture
     * Since the unit of testing here is the aggregate, AggregateTestFixture is meant to test
     * one aggregate only. So, all commands in the when (or given) clause are meant to target
     * the aggregate under test fixture. Also, all given and expected events are meant to be
     * triggered from the aggregate under test fixture.
     */
    private FixtureConfiguration<WorkflowAggregate> fixture;
    @Autowired
    private RuleBaseService ruleBaseService;

    @Before
    public void setup() {
        fixture = new AggregateTestFixture<>(WorkflowAggregate.class);
        fixture.registerInjectableResource(ruleBaseService);
    }

    @Test
    public void testEnableCommand() {
        fixture.given(new CreatedWorkflowEvt("test_wf_1"))
                .when(new EnableCmd("test_wf_1", 0))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new EnabledEvt("test_wf_1", 0));
    }

    @Test
    public void testSnapshotEqualsAggregate() {
        // TODO
    }
}
