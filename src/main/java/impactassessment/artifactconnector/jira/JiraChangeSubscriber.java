package impactassessment.artifactconnector.jira;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jiralightconnector.ChangeSubscriber;
import c4s.jiralightconnector.IssueAgent;
import impactassessment.api.Commands.UpdateArtifactsCmd;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraChangeSubscriber implements ChangeSubscriber {

    private final CommandGateway commandGateway;
    // Map<workflowId, Set<artifactKey>>
    private ConcurrentMap<JiraDataScope, Set<String>> artifactUsages = new ConcurrentHashMap<>();

    public void addUsage(JiraDataScope scope, ArtifactIdentifier id) {
        Set<String> artifactKeys = artifactUsages.get(scope);
        if (artifactKeys == null) {
            Set<String> newSet = new HashSet<>();
            newSet.add(id.getId());
            artifactUsages.put(scope, newSet);
        } else {
            artifactKeys.add(id.getId());
        }
        log.debug("Workflow: {} has following usages: {}", scope.getScopeId(), artifactUsages.get(scope).stream().collect(Collectors.joining( ", " )));
    }

    public void removeUsage(JiraDataScope scope) {
        artifactUsages.remove(scope);
    }

	@Override
	public void handleUpdatedIssues(List<IssueAgent> issues, CorrelationTuple corr) {
        log.info("handleUpdateIssues");
        for (Map.Entry<JiraDataScope, Set<String>> entry : artifactUsages.entrySet()) {
            JiraDataScope scope = entry.getKey();
            Set<String> artifactKeys = entry.getValue();
            List<IArtifact> affectedArtifacts = new ArrayList<>();
            for (IssueAgent ia : issues) {
                if (artifactKeys.stream().anyMatch(key -> key.equals(ia.getKey()))) {
                    affectedArtifacts.add(scope.replaceWithUpdate(ia));
                }
            }
            if (affectedArtifacts.size() > 0) {
                UpdateArtifactsCmd cmd = new UpdateArtifactsCmd(scope.getScopeId(), affectedArtifacts);
                log.debug("send changes to {}", scope.getScopeId());
                commandGateway.sendAndWait(cmd);
            }
        }
	}
}
