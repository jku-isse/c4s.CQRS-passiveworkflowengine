package impactassessment.aggregates;

import impactassessment.command.WorkflowAggregate;
import impactassessment.rulebase.KieSessionService;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(classes = AggregateTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractFixtureTest {

    /**
     * Focus of a Test Fixture
     * Since the unit of testing here is the aggregate, AggregateTestFixture is meant to test
     * one aggregate only. So, all commands in the when (or given) clause are meant to target
     * the aggregate under test fixture. Also, all given and expected events are meant to be
     * triggered from the aggregate under test fixture.
     * (https://docs.axoniq.io/reference-guide/implementing-domain-logic/command-handling/testing)
     */
    FixtureConfiguration<WorkflowAggregate> fixture;
    @Mock
    KieSessionService kieSessionService;
    String id;

    @Before
    public void setup() {
        fixture = new AggregateTestFixture<>(WorkflowAggregate.class);
        // real ruleBaseService:
//        CommandGateway gateway = DefaultCommandGateway.builder()
//                .commandBus(fixture.getCommandBus())
//                .build();
//        ruleBaseService = new RuleBaseService(gateway);
        fixture.registerInjectableResource(kieSessionService);
        id = "Test-Workflow";
    }
}
