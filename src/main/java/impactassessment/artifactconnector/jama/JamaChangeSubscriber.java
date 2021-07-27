package impactassessment.artifactconnector.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.jama.IJamaArtifact;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jamaconnector.IJamaChangeSubscriber;
import c4s.jiralightconnector.IssueAgent;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import impactassessment.api.Commands;
import impactassessment.api.Commands.UpdateArtifactsCmd;
import impactassessment.artifactconnector.jira.JiraArtifact;
import impactassessment.artifactconnector.jira.JiraDataScope;
import impactassessment.artifactconnector.usage.PerProcessArtifactUsagePersistor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JamaChangeSubscriber implements IJamaChangeSubscriber {

    private final CommandGateway commandGateway;
    @Qualifier("jama")
    @Autowired
    private final PerProcessArtifactUsagePersistor usage;
    private ConcurrentMap<String, JamaDataScope> scopes = new ConcurrentHashMap<>();

    @Override
    public void handleChangedJamaItems(Set<JamaItem> set, CorrelationTuple correlationTuple) {
    	log.info("handleUpdateIssues");
        Set<String> scopeIds = new HashSet<>();
        scopeIds.addAll(scopes.keySet());
        scopeIds.addAll(usage.getAllScopeIdentifier());
        for (String scopeId : scopeIds) {
                   	        	
            Set<ArtifactIdentifier> artifactKeys = usage.getUsages(scopeId);
            List<IArtifact> affectedArtifacts = new ArrayList<>();
            for (JamaItem j : set) {
                if (artifactKeys.stream().anyMatch(key -> Integer.parseInt(key.getId())==j.getId())) {
                	JamaDataScope scope = scopes.get(scopeId);
                	if (scope != null) {
                		affectedArtifacts.add(scope.replaceWithUpdate(j));
                	} else { // only in case we boot up the system and this scope is not know yet, do we send an artifact without setting the jirascope, this will be lost in cmd serialization anyway
                		affectedArtifacts.add(new JamaArtifact(j, null));
                	}
                }
            }
            if (affectedArtifacts.size() > 0) {
                UpdateArtifactsCmd cmd = new UpdateArtifactsCmd(scopeId, affectedArtifacts);
                log.debug("send changes to {}", scopeId);
                commandGateway.sendAndWait(cmd);
            }
        }
     }

    public void addUsage(JamaDataScope scope, ArtifactIdentifier id) {
    	usage.addUsage(scope.getScopeId(), id);
        scopes.putIfAbsent(scope.getScopeId(), scope);
    	
    	
//    	Set<Integer> artifactKeys = artifactUsages.get(scope);
//        if (artifactKeys == null) {
//            Set<Integer> newSet = new HashSet<>();
//            newSet.add(Integer.parseInt(id.getId()));
//            artifactUsages.put(scope, newSet);
//        } else {
//            artifactKeys.add(Integer.parseInt(id.getId()));
//        }
//        log.debug("Workflow: {} has following usages: {}", scope.getScopeId(), artifactUsages.get(scope).stream()
//                .map(String::valueOf)
//                .collect(Collectors.joining( ", " )));
    }

    public void removeUsage(JamaDataScope scope) {
    	usage.removeScope(scope.getScopeId());
    }
}
