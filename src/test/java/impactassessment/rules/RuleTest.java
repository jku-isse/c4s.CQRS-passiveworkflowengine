package impactassessment.rules;

import impactassessment.SpringApp;
import impactassessment.api.Events.*;
import impactassessment.exampleworkflows.DronologyWorkflow;
import impactassessment.exampleworkflows.DronologyWorkflowFixed;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.kiesession.KieSessionFactory;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import passiveprocessengine.instance.ResourceLink;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringApp.class)
public class RuleTest {

    private KieSession kieSession;
    private WorkflowInstanceWrapper model;
    @Mock
    private CommandGateway gateway;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        kieSession = new KieSessionFactory().getKieSession("execution.drl", "constraints.drl");
        kieSession.setGlobal("commandGateway", gateway);
        model = new WorkflowInstanceWrapper();
    }

    @Test
    public void testOpenIssueAdd() {
        String id = "A1";
        IJiraArtifact a = JiraMockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_OPEN);
        addArtifact(a);
        int fired = insertAndFire(a);

        assertEquals(1, fired);
    }

    @Test
    public void testOpenIssueAddComplete() {
        String id = "A1";
        IJiraArtifact a = JiraMockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_OPEN);
        addArtifact(a);
        int fired = insertAndFire(a);
        completeDataflow(a);
        fired += insertAndFire(a);

        assertEquals(2, fired);
    }

    @Test
    public void testInProgressIssueAdd() {
        String id = "A2";
        IJiraArtifact a = JiraMockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_IN_PROGRESS);
        addArtifact(a);
        int fired = insertAndFire(a);

        assertEquals(1, fired);
    }

    @Test
    public void testInProgressIssueAddComplete() {
        String id = "A2";
        IJiraArtifact a = JiraMockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_IN_PROGRESS);
        addArtifact(a);
        int fired = insertAndFire(a);
        completeDataflow(a);
        fired += insertAndFire(a);

        assertEquals(3, fired);
    }

    @Test
    public void testResolvedIssueAdd() {
        String id = "A3";
        IJiraArtifact a = JiraMockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_RESOLVED);
        addArtifact(a);
        int fired = insertAndFire(a);

        assertEquals(1, fired);
    }

    @Test
    public void testResolvedIssueAddComplete() {
        String id = "A3";
        IJiraArtifact a = JiraMockService.mockArtifact(id, DronologyWorkflow.TASK_STATE_RESOLVED);
        addArtifact(a);
        int fired = insertAndFire(a);
        completeDataflow(a);
        fired += insertAndFire(a);

        assertEquals(3, fired);
    }

    @After
    public void tearDown() {
        kieSession.dispose();
        model = null;
    }

    private void addArtifact(IJiraArtifact a) {
        model.handle(new CreatedWorkflowEvt(a.getKey(), List.of(a), "x", new DronologyWorkflowFixed()));
    }

    private void completeDataflow(IJiraArtifact a) {
        String id = a.getKey();
        model.handle(new CompletedDataflowEvt(id, "workflowKickOff#"+id, new ResourceLink(a.getKey(), "test", "test", "test", "test", "test")));
    }

    private int insertAndFire(IJiraArtifact a) {
        kieSession.insert(a);
        kieSession.insert(model.getWorkflowInstance());
        model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                .forEach(wft -> kieSession.insert(wft));
        model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                .forEach(dni -> kieSession.insert(dni));
        return kieSession.fireAllRules();
    }
}
