package impactassessment.query;

import artifactapi.IArtifact;
import impactassessment.api.Commands.*;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import passiveprocessengine.definition.Artifact;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.instance.QACheckDocument;
import passiveprocessengine.instance.RuleEngineBasedConstraint;
import passiveprocessengine.instance.WorkflowWrapperTaskInstance;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactWrapper;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
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
            kieSessions.setInitialized(id);
        }
    }

    public static void createSubWorkflow(CommandGateway commandGateway, WorkflowWrapperTaskInstance wwti, String wfiId) {
//        List<IArtifact> artifacts = wwti.getInput().stream()
//                .filter(ai -> ai.getArtifact() instanceof ArtifactWrapper)
//                .map(ai -> ((ArtifactWrapper)ai.getArtifact()).getWrappedArtifact())
//                .filter(o -> o instanceof IArtifact)
//                .map(o -> (IArtifact)o)
//                .collect(Collectors.toList());
        
        Collection<Entry<String,IArtifact>> artifacts = wwti.getInput().stream()
        		.map(in -> { 	
        			if (in.getArtifact() instanceof IArtifact)
        				return new AbstractMap.SimpleEntry<String, IArtifact>(in.getRole(), (IArtifact)in.getArtifact());
        			if (in.getArtifact() instanceof ArtifactWrapper) {
        				Object wa = ((ArtifactWrapper) in.getArtifact()).getWrappedArtifact();
        				if (wa instanceof IArtifact)
        					return new AbstractMap.SimpleEntry<String, IArtifact>(in.getRole(), (IArtifact)wa);
        			}
        			return null;
        		})
        		.filter(Objects::nonNull)
        		.collect(Collectors.toList());
        // THis approach is not quite ok, as the artifact could be set/updated later and the datamapping among different role names is not considered
        // FIXME: proper subwp handling, i.e. input and output mapping propagation
        CreateSubWorkflowCmd cmd = new CreateSubWorkflowCmd(wwti.getSubWfiId(), wfiId, wwti.getId(), wwti.getSubWfdId(), artifacts);
        commandGateway.send(cmd);
    }
    
    

    public static void addToSubWorkflow(CommandGateway commandGateway, IWorkflowTask wft, ArtifactInput ai) {
        if (wft instanceof WorkflowWrapperTaskInstance) {
            WorkflowWrapperTaskInstance wwti = (WorkflowWrapperTaskInstance) wft;
            commandGateway.send(new AddInputToWorkflowCmd(wwti.getSubWfiId(), ai));
        }
    }

    public static IArtifact checkIfIArtifactInside(Artifact artifact) {
        if (artifact instanceof ArtifactWrapper) {
            ArtifactWrapper artifactWrapper = (ArtifactWrapper) artifact;
            if (artifactWrapper.getWrappedArtifact() instanceof IArtifact) {
                return (IArtifact) artifactWrapper.getWrappedArtifact();
            }
        }
        return null;
    }
}
