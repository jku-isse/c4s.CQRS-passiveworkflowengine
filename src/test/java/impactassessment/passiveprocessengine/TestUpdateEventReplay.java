package impactassessment.passiveprocessengine;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.ResourceLink;
import artifactapi.jama.IJamaArtifact;
import impactassessment.SpringConfig;
import impactassessment.api.Events;
import impactassessment.api.Queries;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.IJamaService;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.MockKieSessionService;
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
import passiveprocessengine.instance.CorrelationTuple;
import passiveprocessengine.instance.WorkflowInstance;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestUpdateEventReplay {

    private IJiraService jiraS;
    private IJamaService jamaS;
    private JiraChangeSubscriber jiraCS;
    private JamaChangeSubscriber jamaCS;
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

        jiraCS = new JiraChangeSubscriber(gw, null);
        jiraS = conf.getJiraService(conf.getJiraInstance(conf.getJiraCache(), jiraCS, conf.getJiraMonitoringState()), jiraCS);
        aRegistry.register(jiraS);
        jamaCS = new JamaChangeSubscriber(gw, null);
        jamaS = conf.getJamaService(conf.getJamaInstance(conf.getJamaCache()), jamaCS);
        aRegistry.register(jamaS);

        MockKieSessionService kieS = new MockKieSessionService();
//        SimpleKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);

        SimpleFrontendPusher fp = new SimpleFrontendPusher();

        wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry, new EventList2Forwarder());
        gw.setWorkflowProjection(wfp);
    }

    @Test
    public void test() throws NoSuchFieldException, IllegalAccessException {
        ReplayStatus status = ReplayStatus.REPLAY; // important to set replay status to REPLAY!!
        String id = "TestId1";
        int jamaId = 14464163;

        // event replay
        wfp.on(new Events.CreatedWorkflowEvt(
                id,
                Map.of(new ArtifactIdentifier(jamaId+"", "IJamaArtifact"), "jama"),
                "DemoProcess2",
                registry.get("DemoProcess2").getWfd()
        ), status);
        wfp.on(new Events.AddedConstraintsEvt(
                id,
                "Evaluate#"+id,
                Map.of("CheckJiraExists", "Is there a Jira issue linked?")
        ), status);
        wfp.on(new Events.AddedEvaluationResultToConstraintEvt(
                id,
                "Evaluate#"+id,
                "CheckJiraExists_Evaluate_"+id,
                Map.of(new ResourceLink("test", "test", "test", "test", "test", "test"), false),
                new CorrelationTuple(),
                Instant.now()
        ), status);

        status = ReplayStatus.REGULAR;

        // new updated jama artifact instance
        IJamaArtifact updatedJamaArt =jamaS.get(jamaId).get();
        Field nameField = updatedJamaArt.getClass().getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(updatedJamaArt, "UPDATED#"+updatedJamaArt.getName());

        // normal event(s)
        wfp.on(new Events.UpdatedArtifactsEvt(
                id,
                List.of(new ArtifactIdentifier(jamaId+"", "IJamaArtifact"))
                ));
        wfp.on(new Events.AddedEvaluationResultToConstraintEvt(
                id,
                "Evaluate#"+id,
                "CheckJiraExists_Evaluate_"+id,
                Map.of(updatedJamaArt.convertToResourceLink(), true),
                new CorrelationTuple(),
                Instant.now()
        ), status);

        WorkflowInstance wfi = getWfi();


        System.out.println("done");
    }

    private WorkflowInstance getWfi() {
        Collection<WorkflowInstance> state = wfp.handle(new Queries.GetStateQuery("*")).getState();
        assertEquals(1, state.size());
        for (WorkflowInstance wfi : state) {
            return wfi;
        }
        return null;
    }

}
