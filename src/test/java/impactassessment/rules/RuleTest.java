package impactassessment.rules;

import impactassessment.api.AddedArtifactEvt;
import impactassessment.api.CompletedDataflowEvt;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import impactassessment.model.WorkflowModel;
import impactassessment.model.definition.DronologyWorkflow;
import impactassessment.rulebase.RuleBaseFactory;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

@XSlf4j
public class RuleTest {

    private KieSession kieSession;
    private WorkflowModel model;
    @Mock
    private CommandGateway gateway;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        kieSession = new RuleBaseFactory().getKieSession();
        kieSession.setGlobal("commandGateway", gateway);
        model = new WorkflowModel();
    }

    @Test
    public void testOpenIssueAdd() {
        String id = "A1";
        Artifact a = MockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_OPEN);
        addArtifact(a);
        int fired = insertAndFire(a);

        assertEquals(1, fired);
    }

    @Test
    public void testOpenIssueAddComplete() {
        String id = "A1";
        Artifact a = MockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_OPEN);
        addArtifact(a);
        int fired = insertAndFire(a);
        completeDataflow(a);
        fired += insertAndFire(a);

        assertEquals(1, fired);
    }

    @Test
    public void testInProgressIssueAdd() {
        String id = "A2";
        Artifact a = MockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_IN_PROGRESS);
        addArtifact(a);
        int fired = insertAndFire(a);

        assertEquals(1, fired);
    }

    @Test
    public void testInProgressIssueAddComplete() {
        String id = "A2";
        Artifact a = MockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_IN_PROGRESS);
        addArtifact(a);
        int fired = insertAndFire(a);
        completeDataflow(a);
        fired += insertAndFire(a);

        assertEquals(2, fired);
    }

    @Test
    public void testResolvedIssueAdd() {
        String id = "A3";
        Artifact a = MockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_RESOLVED);
        addArtifact(a);
        int fired = insertAndFire(a);

        assertEquals(1, fired);
    }

    @Test
    public void testResolvedIssueAddComplete() {
        String id = "A3";
        Artifact a = MockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_RESOLVED);
        addArtifact(a);
        int fired = insertAndFire(a);
        completeDataflow(a);
        fired += insertAndFire(a);

        assertEquals(2, fired);
    }

    @After
    public void tearDown() {
        kieSession.dispose();
        model = null;
    }

    private void addArtifact(Artifact a) {
        model.handle(new AddedArtifactEvt(a.getId(), a));
    }

    private void completeDataflow(Artifact a) {
        String id = a.getId();
        model.handle(new CompletedDataflowEvt(id, "workflowKickOff#"+id, a));
    }

    private int insertAndFire(Artifact a) {
        kieSession.insert(a);
        kieSession.insert(model.getWorkflowInstance());
        model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                .forEach(wft -> kieSession.insert(wft));
        model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                .forEach(dni -> kieSession.insert(dni));
        return kieSession.fireAllRules();
    }
}
