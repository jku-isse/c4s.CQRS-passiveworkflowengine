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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraChangeSubscriber implements ChangeSubscriber {

    private final CommandGateway commandGateway;
    // Map<workflowId, Set<artifactKey>>
    private Map<String, Set<String>> artifactUsages = new HashMap<>();

    @Override
    public void handleUpdatedIssues(List<IssueAgent> list, CorrelationTuple corr) {
        log.info("handleUpdateIssues");
        for (Map.Entry<String, Set<String>> entry : artifactUsages.entrySet()) {
            String workflowId = entry.getKey();
            Set<String> artifactKeys = entry.getValue();
            List<IArtifact> affectedArtifacts = new ArrayList<>();
            for (IssueAgent ia : list) {
                if (artifactKeys.stream().anyMatch(key -> key.equals(ia.getKey()))) {
                    affectedArtifacts.add(new JiraArtifact(ia.getIssue()));
                }
            }
            if (affectedArtifacts.size() > 0) {
                UpdateArtifactsCmd cmd = new UpdateArtifactsCmd(workflowId, affectedArtifacts);
                commandGateway.sendAndWait(cmd);
            }
        }
    }

    public void addUsage(String workflowId, ArtifactIdentifier id) {
        Set<String> artifactKeys = artifactUsages.get(workflowId);
        if (artifactKeys == null) {
            Set<String> newSet = new HashSet<>();
            newSet.add(id.getId());
            artifactUsages.put(workflowId, newSet);
        } else {
            artifactKeys.add(id.getId());
        }
        log.debug("Workflow: {} has following usages: {}", workflowId, artifactUsages.get(workflowId).stream().collect(Collectors.joining( ", " )));
    }
}
