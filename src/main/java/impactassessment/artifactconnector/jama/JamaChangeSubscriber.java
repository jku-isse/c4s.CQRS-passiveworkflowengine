package impactassessment.artifactconnector.jama;

import artifactapi.ArtifactIdentifier;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.IJamaChangeSubscriber;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JamaChangeSubscriber implements IJamaChangeSubscriber {

    private Map<String, Set<ArtifactIdentifier>> artifactUsages = new HashMap<>();
    private final CommandGateway commandGateway;

    @Override
    public void handleChangedJamaItems(Set<JamaItem> set, CorrelationTuple correlationTuple) {
        // TODO send UpdateArtifactsCmds to affected workflows
        log.info("not implemented");
    }

    public void addUsage(String workflowId, ArtifactIdentifier artifactKey) {
        Set<ArtifactIdentifier> artifactKeys = artifactUsages.get(workflowId);
        if (artifactKeys == null) {
            Set<ArtifactIdentifier> newSet = new HashSet<>();
            newSet.add(artifactKey);
            artifactUsages.put(workflowId, newSet);
        } else {
            artifactKeys.add(artifactKey);
        }
        log.debug("Workflow: {} has following usages: {}", workflowId, artifactUsages.get(workflowId).stream()
                .map(ArtifactIdentifier::getId)
                .collect(Collectors.joining( ", " )));
    }
}
