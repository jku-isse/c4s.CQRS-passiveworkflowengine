package impactassessment;

import artifactapi.IArtifact;
import at.jku.designspace.sdk.clientservice.IDesignspaceChangeSubscriber;
import impactassessment.api.Commands;
import impactassessment.artifactconnector.jira.JiraDataScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DesignspaceChangeSubscriber implements IDesignspaceChangeSubscriber {

    private final CommandGateway commandGateway;

    @Override
    public void handleUpdatedInstances(Collection<IArtifact> changedArtifacts, String scopeId) {
        log.info("handling updated issues");
        List<IArtifact> affectedArtifacts = new ArrayList<>(changedArtifacts);

        if (affectedArtifacts.size() > 0) {
            Commands.UpdateArtifactsCmd cmd = new Commands.UpdateArtifactsCmd(scopeId, affectedArtifacts);
            log.debug("send changes to {}", scopeId);
            commandGateway.sendAndWait(cmd);
        }
    }

}
