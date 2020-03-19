package impactassessment.snapshots;

import impactassessment.api.*;
import impactassessment.command.WorkflowAggregate;
import impactassessment.rulebase.RuleBaseService;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(classes = SpringTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSnapshot {

    /**
     * Focus of a Test Fixture
     * Since the unit of testing here is the aggregate, AggregateTestFixture is meant to test
     * one aggregate only. So, all commands in the when (or given) clause are meant to target
     * the aggregate under test fixture. Also, all given and expected events are meant to be
     * triggered from the aggregate under test fixture.
     * (https://docs.axoniq.io/reference-guide/implementing-domain-logic/command-handling/testing)
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
    public void testCreateCommand() {
        fixture.givenNoPriorActivity()
                .when(new CreateWorkflowCmd("hi"))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testEnableCommand() {
        fixture.given(new CreatedWorkflowEvt("test_wf"))
                .when(new EnableCmd("test_wf", 0))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new EnabledEvt("test_wf", 0));
    }

    @Test
    public void testCompleteCommand() {
        fixture.given(new CreatedWorkflowEvt("test_wf"), new EnableCmd("test_wf", 0))
                .when(new CompleteCmd("test_wf"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new CompletedEvt("test_wf"));
    }

    @Test
    public void testSnapshotEqualsAggregate() {
        // TODO
    }

}
