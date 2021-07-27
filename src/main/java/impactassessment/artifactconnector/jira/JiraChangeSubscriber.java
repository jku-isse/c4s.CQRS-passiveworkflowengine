package impactassessment.artifactconnector.jira;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import c4s.analytics.monitoring.tracemessages.CorrelationTuple;
import c4s.jiralightconnector.ChangeSubscriber;
import c4s.jiralightconnector.IssueAgent;
import impactassessment.api.Commands.UpdateArtifactsCmd;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraChangeSubscriber implements ChangeSubscriber {

    private final CommandGateway commandGateway;
    @Qualifier("jira")
    @Autowired
    private final PerProcessArtifactUsagePersistor usage;
    // Map<workflowId, Set<artifactKey>>
    //private ConcurrentMap<JiraDataScope, Set<String>> artifactUsages = new ConcurrentHashMap<>();
    private ConcurrentMap<String, JiraDataScope> scopes = new ConcurrentHashMap<>();
    
    public void addUsage(JiraDataScope scope, ArtifactIdentifier id) {
        usage.addUsage(scope.getScopeId(), id);
        scopes.putIfAbsent(scope.getScopeId(), scope);
    	
    	
//    	Set<String> artifactKeys = artifactUsages.get(scope);
//        if (artifactKeys == null) {
//            Set<String> newSet = new HashSet<>();
//            newSet.add(id.getId());
//            artifactUsages.put(scope, newSet);
//        } else {
//            artifactKeys.add(id.getId());
//        }
//        log.debug("Workflow: {} has following usages: {}", scope.getScopeId(), artifactUsages.get(scope).stream().collect(Collectors.joining( ", " )));
    }

    public void removeUsage(JiraDataScope scope) {
        usage.removeScope(scope.getScopeId());    
    }

	@Override
	public void handleUpdatedIssues(List<IssueAgent> issues, CorrelationTuple corr) {
        log.info("handleUpdateIssues");
        Set<String> scopeIds = new HashSet<>();
        scopeIds.addAll(scopes.keySet());
        scopeIds.addAll(usage.getAllScopeIdentifier());
        for (String scopeId : scopeIds) {
                   	        	
            Set<ArtifactIdentifier> artifactKeys = usage.getUsages(scopeId);
            List<IArtifact> affectedArtifacts = new ArrayList<>();
            for (IssueAgent ia : issues) {
                if (artifactKeys.stream().anyMatch(key -> key.getId().equals(ia.getKey()))) {
                	JiraDataScope scope = scopes.get(scopeId);
                	if (scope != null) {
                		affectedArtifacts.add(scope.replaceWithUpdate(ia));
                	} else { // only in case we boot up the system and this scope is not know yet, do we send an artifact without setting the jirascope, this will be lost in cmd serialization anyway
                		affectedArtifacts.add(new JiraArtifact(ia.getIssue(), null));
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
}
