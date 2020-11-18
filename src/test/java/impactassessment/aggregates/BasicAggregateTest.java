package impactassessment.aggregates;

import impactassessment.SpringApp;
import impactassessment.api.Events.*;
import impactassessment.api.Commands.*;
import impactassessment.command.WorkflowAggregate;
import impactassessment.exampleworkflows.DronologyWorkflowFixed;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.kiesession.KieSessionService;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import passiveprocessengine.instance.ResourceLink;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringApp.class)
public class BasicAggregateTest {

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

    @Test
    public void testAddCompleteActivate() {
        IJiraArtifact a = JiraMockService.mockArtifact(id);
        fixture.given(new CreatedWorkflowEvt(id, List.of(a), "test", new DronologyWorkflowFixed()))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, new ResourceLink("test", "test", "test", "test", "test", "test")))
                .when(new ActivateInOutBranchCmd(id, "open2inProgressOrResolved#"+id, "Open#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testDelete() {
        IJiraArtifact a = JiraMockService.mockArtifact(id);
        fixture.given(new CreatedWorkflowEvt(id, List.of(a), "test", new DronologyWorkflowFixed()))
                .when(new DeleteCmd(id))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new DeletedEvt(id))
                .expectMarkedDeleted();
    }

}
