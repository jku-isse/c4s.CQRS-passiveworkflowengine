package impactassessment.query;

import impactassessment.api.Commands.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.kiesession.KieSessionService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieContainer;
import passiveprocessengine.definition.Artifact;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.instance.AbstractWorkflowInstanceObject;
import passiveprocessengine.instance.QACheckDocument;
import passiveprocessengine.instance.RuleEngineBasedConstraint;
import passiveprocessengine.instance.WorkflowWrapperTaskInstance;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactWrapper;

import java.util.List;

@Slf4j
public class WorkflowHelpers {

    static void createKieSession(KieSessionService kieSessions, ProjectionModel projection, String id, KieContainer kieContainer) {
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(id);
        if (!kieSessions.isInitialized(id) && wfiWrapper == null) { // if artifact is only updated wfiWrapper won't be null anymore
            kieSessions.create(id, kieContainer);
        }
    }

    static void ensureInitializedKB(KieSessionService kieSessions, ProjectionModel projection, String id) {
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(id);
        if (!kieSessions.isInitialized(id) && wfiWrapper != null) {
            List<IJiraArtifact> artifacts = wfiWrapper.getArtifacts();
            log.info(">>INIT KB<<");
            // if kieSession is not initialized, try to add all artifacts
            for (IJiraArtifact artifact : artifacts) {
                kieSessions.insertOrUpdate(id, artifact);
            }
            wfiWrapper.getWorkflowInstance().getWorkflowTasksReadonly()
                    .forEach(wft -> {
                        kieSessions.insertOrUpdate(id, wft);
                        QACheckDocument doc = wfiWrapper.getQACDocOfWft(wft.getTaskId());
                        if (doc != null) {
                            kieSessions.insertOrUpdate(id, doc);
                            doc.getConstraintsReadonly().stream()
                                    .filter(q -> q instanceof RuleEngineBasedConstraint)
                                    .map(q -> (RuleEngineBasedConstraint) q)
                                    .forEach(rebc -> kieSessions.insertOrUpdate(id, rebc));
                        }
                    });
            wfiWrapper.getWorkflowInstance().getDecisionNodeInstancesReadonly()
                    .forEach(dni -> kieSessions.insertOrUpdate(id, dni));
            kieSessions.setInitialized(id);
        }
    }

    static void createSubWorkflow(CommandGateway commandGateway, List<AbstractWorkflowInstanceObject> awos, String wfiId, boolean isReplay) {
        if (!isReplay) {
            awos.stream()
                    .filter(awo -> awo instanceof WorkflowWrapperTaskInstance)
                    .map(awo -> (WorkflowWrapperTaskInstance) awo)
                    .forEach(wwti -> {
//                    Optional<IJiraArtifact> optIJira = wwti.getInput().stream()
//                            .filter(ai -> ai.getArtifact() instanceof ArtifactWrapper)
//                            .map(ai -> ((ArtifactWrapper)ai.getArtifact()).getWrappedArtifact())
//                            .filter(o -> o instanceof IJiraArtifact)
//                            .map(o -> (IJiraArtifact)o)
//                            .findAny();
//                    if (optIJira.isPresent()) {
//
//                    } else {
                        commandGateway.send(new CreateSubWorkflowCmd(wwti.getSubWfiId(), wfiId, wwti.getId(), wwti.getSubWfdId()));
//                    }
                    });
        }
    }

    static void addToSubWorkflow(CommandGateway commandGateway, IWorkflowTask wft, ArtifactInput ai) {
        if (wft instanceof WorkflowWrapperTaskInstance) {
            WorkflowWrapperTaskInstance wwti = (WorkflowWrapperTaskInstance) wft;
            commandGateway.send(new AddInputToWorkflowCmd(wwti.getSubWfiId(), ai));
        }
    }

    static void insertOrUpdateKieSession(KieSessionService kieSessions, String id, List<AbstractWorkflowInstanceObject> awos, List<IJiraArtifact> artifacts, boolean isReplay) {
        if (!isReplay) {
            if (artifacts != null)
                artifacts.forEach(a -> kieSessions.insertOrUpdate(id, a));
            if (awos != null)
                awos.forEach(awo -> kieSessions.insertOrUpdate(id, awo));
            kieSessions.fire(id);
        }
    }

    static IJiraArtifact checkIfJiraArtifactInside(Artifact artifact) {
        if (artifact instanceof ArtifactWrapper) {
            ArtifactWrapper artifactWrapper = (ArtifactWrapper) artifact;
            if (artifactWrapper.getWrappedArtifact() instanceof IJiraArtifact) {
                return (IJiraArtifact) artifactWrapper.getWrappedArtifact();
            }
        }
        return null;
    }
}
