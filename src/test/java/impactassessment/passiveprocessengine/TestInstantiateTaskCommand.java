package impactassessment.passiveprocessengine;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jira.IJiraArtifact;
import impactassessment.SpringConfig;
import impactassessment.api.Events;
import impactassessment.api.Queries;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.IJamaService;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.EventList2Forwarder;
import impactassessment.query.NoOpHistoryLogEventLogger;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.SimpleFrontendPusher;
import org.axonframework.eventhandling.ReplayStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.WorkflowInstance;
import passiveprocessengine.instance.WorkflowTask;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class TestInstantiateTaskCommand {

    private IJiraService jiraS;
    private IJamaService jamaS;
    private WorkflowDefinitionRegistry registry;
    private WorkflowProjection wfp;

    @BeforeEach
    public void setup() {
        SpringConfig conf = new SpringConfig();

        registry = new WorkflowDefinitionRegistry();
        LocalRegisterService lrs = new LocalRegisterService(registry);
        lrs.registerAll();

        IArtifactRegistry aRegistry = new ArtifactRegistry();
        ProjectionModel pModel = new ProjectionModel(aRegistry);
        MockCommandGateway gw = new MockCommandGateway(aRegistry, registry);

        JiraChangeSubscriber jiraCS = new JiraChangeSubscriber(gw, null);
        jiraS = conf.getJiraService(conf.getJiraInstance(conf.getJiraCache(), jiraCS, conf.getJiraMonitoringState()), jiraCS);
        aRegistry.register(jiraS);
        JamaChangeSubscriber jamaCS = new JamaChangeSubscriber(gw, null);
        jamaS = conf.getJamaService(conf.getJamaInstance(conf.getJamaCache()), jamaCS);
        aRegistry.register(jamaS);

        SimpleKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);

        SimpleFrontendPusher fp = new SimpleFrontendPusher();

        wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry, new EventList2Forwarder());
        gw.setWorkflowProjection(wfp);
    }

    @Test
    public void testInstantiateTask() {
        ReplayStatus status = ReplayStatus.REGULAR;
        String id = "TestId1";

        IJamaArtifact jamaArt = (IJamaArtifact) jamaS.get(new ArtifactIdentifier("14464163", "IJamaArtifact"), id).get();
        wfp.on(new Events.CreatedWorkflowEvt(id, Map.of(new ArtifactIdentifier("14464163", "IJamaArtifact"), "jama"), "DemoProcess2", registry.get("DemoProcess2").getWfd()), status);

        IJiraArtifact jiraArt = (IJiraArtifact) jiraS.get(new ArtifactIdentifier("DEMO-9", "IJiraArtifact"), id).get();
        ArtifactInput in = new ArtifactInput(jiraArt, "jira");
        wfp.on(new Events.InstantiatedTaskEvt(id, "Evaluate", List.of(in), Collections.emptyList()), status); // should be ignored (task already exists!)
        wfp.on(new Events.InstantiatedTaskEvt(id, "Execute", List.of(in), Collections.emptyList()), status);

        Collection<WorkflowInstance> state = wfp.handle(new Queries.GetStateQuery("*")).getState();
        assertEquals(1, state.size());
        for (WorkflowInstance wfi : state) {
            assertEquals(2, wfi.getWorkflowTasksReadonly().size()); // Task "Evaluate" and "Execute" should be present
            WorkflowTask wft = wfi.getWorkflowTasksReadonly().stream()
                    .filter(x -> x.getId().startsWith("Execute"))
                    .findAny()
                    .get();
            assertEquals(1, wft.getOutput().size()); // QACheckDocument
            assertEquals(1, wft.getInput().size()); // IJiraArtifact (DEMO-9)
        }

        System.out.println("done");
    }

    @Test
    public void testInstantiateTaskAndRegualrEnabling() {
        ReplayStatus status = ReplayStatus.REGULAR;
        String id = "TestId1";

        IJamaArtifact jamaArt = (IJamaArtifact) jamaS.get(new ArtifactIdentifier("14464163", "IJamaArtifact"), id).get();
        wfp.on(new Events.CreatedWorkflowEvt(id, Map.of(new ArtifactIdentifier("14464163", "IJamaArtifact"), "jama"), "DemoProcess2", registry.get("DemoProcess2").getWfd()), status);

        IJiraArtifact jiraArt = (IJiraArtifact) jiraS.get(new ArtifactIdentifier("DEMO-9", "IJiraArtifact"), id).get();
        ArtifactInput in = new ArtifactInput(jiraArt, "jira");
        wfp.on(new Events.InstantiatedTaskEvt(id, "Evaluate", List.of(in), Collections.emptyList()), status); // should be ignored (task already exists!)
        wfp.on(new Events.InstantiatedTaskEvt(id, "Execute", List.of(in), Collections.emptyList()), status);
        wfp.on(new Events.AddedOutputEvt(id, "Evaluate#"+id, new ArtifactIdentifier("DEMO-9", "IJiraArtifact"), "checkissue"), status);

        // --> Task Evaluate#TestId1 received (and ignored) for 'expectedSM' unexpected Event ACTIVATE for State ACTIVE

        Collection<WorkflowInstance> state = wfp.handle(new Queries.GetStateQuery("*")).getState();
        assertEquals(1, state.size());
        for (WorkflowInstance wfi : state) {
            assertEquals(2, wfi.getWorkflowTasksReadonly().size()); // Task "Evaluate" and "Execute" should be present
            WorkflowTask wft = wfi.getWorkflowTasksReadonly().stream()
                    .filter(x -> x.getId().startsWith("Execute"))
                    .findAny()
                    .get();
            assertEquals(1, wft.getOutput().size()); // QACheckDocument
            assertEquals(1, wft.getInput().size()); // IJiraArtifact (DEMO-9)
        }

        System.out.println("done");
    }

}
