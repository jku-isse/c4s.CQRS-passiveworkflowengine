package impactassessment.artifactconnector.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.IJamaChangeSubscriber;
import c4s.jiralightconnector.IssueAgent;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import impactassessment.api.Commands;
import impactassessment.artifactconnector.jira.JiraArtifact;
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
public class JamaChangeSubscriber implements IJamaChangeSubscriber {

    private ConcurrentMap<JamaDataScope, Set<Integer>> artifactUsages = new ConcurrentHashMap<>();
    private final CommandGateway commandGateway;

    @Override
    public void handleChangedJamaItems(Set<JamaItem> set, CorrelationTuple correlationTuple) {
        for (Map.Entry<JamaDataScope, Set<Integer>> entry : artifactUsages.entrySet()) {
        	JamaDataScope scope = entry.getKey();
            Set<Integer> artifactKeys = entry.getValue();
            List<IArtifact> affectedArtifacts = new ArrayList<>();
            for (JamaItem j : set) {
                if (artifactKeys.stream().anyMatch(key -> key.equals(j.getId()))) {
                    affectedArtifacts.add(scope.replaceWithUpdate(j));
                }
            }
            if (affectedArtifacts.size() > 0) {
                Commands.UpdateArtifactsCmd cmd = new Commands.UpdateArtifactsCmd(scope.getScopeId(), affectedArtifacts);
                System.out.println("send changes to " + scope.getScopeId());
                commandGateway.sendAndWait(cmd);
            }
        }
    }

    public void addUsage(JamaDataScope scope, ArtifactIdentifier id) {
        Set<Integer> artifactKeys = artifactUsages.get(scope);
        if (artifactKeys == null) {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(Integer.parseInt(id.getId()));
            artifactUsages.put(scope, newSet);
        } else {
            artifactKeys.add(Integer.parseInt(id.getId()));
        }
//        log.debug("Workflow: {} has following usages: {}", workflowId, artifactUsages.get(workflowId).stream()
//                .map(String::valueOf)
//                .collect(Collectors.joining( ", " )));
    }
}
