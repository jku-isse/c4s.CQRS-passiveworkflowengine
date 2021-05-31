package impactassessment.query;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import impactassessment.api.Commands.*;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.instance.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
public class WorkflowHelpers {

    static void ensureInitializedKB(IKieSessionService kieSessions, ProjectionModel projection, String id) {
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(id);
        if (!kieSessions.isInitialized(id) && wfiWrapper != null) {
            List<IArtifact> artifacts = wfiWrapper.getArtifacts();
            log.info(">>INIT KB<<");
            // if kieSession is not initialized, try to add all artifacts
            for (IArtifact artifact : artifacts) {
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
            kieSessions.insertOrUpdate(id, wfiWrapper.getWorkflowInstance());
            kieSessions.setInitialized(id);
        }
    }

    public static void createSubWorkflow(CommandGateway commandGateway, WorkflowWrapperTaskInstance wwti, String wfiId) {
        Map<ArtifactIdentifier, String> arts = new HashMap<>();
        for (ArtifactInput input : wwti.getInput()) {
            for (IArtifact a : input.getArtifacts()) {
                arts.put(a.getArtifactIdentifier(), input.getRole());
            }
        }
        // This approach is not quite ok, as the artifact could be set/updated later and the datamapping among different role names is not considered
        // FIXME: proper subwp handling, i.e. input and output mapping propagation
        CreateSubWorkflowCmd cmd = new CreateSubWorkflowCmd(wwti.getSubWfiId(), wfiId, wwti.getId(), wwti.getSubWfdId(), arts);
        commandGateway.send(cmd);
    }
    
    

    public static void addToSubWorkflow(CommandGateway commandGateway, IWorkflowTask wft, String role, String type) {
        if (wft instanceof WorkflowWrapperTaskInstance) {
            WorkflowWrapperTaskInstance wwti = (WorkflowWrapperTaskInstance) wft;
            commandGateway.send(new AddInputToWorkflowCmd(wwti.getSubWfiId(), wwti.getId(), role, type));
        }
    }

}
