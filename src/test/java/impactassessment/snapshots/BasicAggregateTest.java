package impactassessment.snapshots;

import impactassessment.api.*;
import impactassessment.command.WorkflowAggregate;
import impactassessment.rulebase.RuleBaseService;
import impactassessment.workflowmodel.ResourceLink;
import impactassessment.workflowmodel.definition.QACheckDocument;
import impactassessment.workflowmodel.definition.RuleEngineBasedConstraint;
import impactassessment.workflowmodel.definition.WPManagementWorkflow;
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
    private WPManagementWorkflow workflow;
    private String id;

    @Before
    public void setup() {
        fixture = new AggregateTestFixture<>(WorkflowAggregate.class);
        fixture.registerInjectableResource(ruleBaseService);
        workflow = new WPManagementWorkflow();
        workflow.initWorkflowSpecification();
        workflow.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        id = "test_wf";
    }

    @Test
    public void testCreateWorkflowCommandOnly() {
        fixture.givenNoPriorActivity()
                .when(new CreateWorkflowCmd(id))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    public void testCommandsCreateEnableCompleteAddCreate() {
        QACheckDocument.QAConstraint qac = mockQAConstraint();
        fixture.given(new CreatedWorkflowEvt(id))
                .andGiven(new CreatedWorkflowInstanceOfEvt(id, workflow))
                .andGiven(new EnabledTasksAndDecisionsEvt(id))
                .andGiven(new CompletedDataflowOfDecisionNodeInstanceEvt(id, 0))
                .andGiven(new AddedQAConstraintsAsArtifactOutputsEvt(id, qac))
                .when(new CreateConstraintTriggerCmd(id))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new CreatedConstraintTriggerEvt(id));
    }

    @Test
    public void testDeleteCommand() {
        fixture.given(new CreatedWorkflowEvt(id), new CreatedWorkflowInstanceOfEvt(id, workflow))
                .when(new DeleteCmd(id))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new DeletedEvt(id))
                .expectMarkedDeleted();
    }

    private QACheckDocument.QAConstraint mockQAConstraint() {
        return new RuleEngineBasedConstraint("REBC", null, "EvaluationRule", null, "This is a simple evaluation test!");
    }
}
