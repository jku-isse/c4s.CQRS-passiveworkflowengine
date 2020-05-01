package impactassessment.snapshots;

import impactassessment.api.*;
import impactassessment.command.WorkflowAggregate;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
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
public class BasicAggregateTest {

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
    private String id;

    @Before
    public void setup() {
        fixture = new AggregateTestFixture<>(WorkflowAggregate.class);
        fixture.registerInjectableResource(ruleBaseService);
        id = "Test-Workflow";
    }

    @Test
    public void testAdd() {
        Artifact a = MockService.mockArtifact(id);
        fixture.givenNoPriorActivity()
                .when(new AddArtifactCmd(id, a))
                .expectEvents(new AddedArtifactEvt(id, a));
    }

    @Test
    public void testAddCompleteActivate() {
        Artifact a = MockService.mockArtifact(id);
        fixture.given(new AddedArtifactEvt(id, a))
                .andGiven(new CompletedDataflowEvt(id,"workflowKickOff#"+id, a))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "Open#"+id))
                .andGiven(new ActivatedInBranchEvt(id, "open2inProgressOrResolved#"+id, "inProgressIn"))
                .when(new ActivateOutBranchCmd(id, "open2inProgressOrResolved#"+id, "resolvedIn"))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testDelete() {
        Artifact a = MockService.mockArtifact(id);
        fixture.given(new AddedArtifactEvt(id, a))
                .when(new DeleteCmd(id))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new DeletedEvt(id))
                .expectMarkedDeleted();
    }

}
