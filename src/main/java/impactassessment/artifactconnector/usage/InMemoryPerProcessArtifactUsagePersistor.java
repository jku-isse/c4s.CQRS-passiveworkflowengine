package impactassessment.artifactconnector.usage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import artifactapi.ArtifactIdentifier;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemoryPerProcessArtifactUsagePersistor implements PerProcessArtifactUsagePersistor{

	private HashMap<String, UsageDTO> usages = new HashMap<>();
	
	@Override
	public void addUsage(String projectScopeId, ArtifactIdentifier ai) {
		UsageDTO usage = usages.computeIfAbsent(projectScopeId, k-> new UsageDTO(projectScopeId));
		usage.getUsages().add(ai);
        log.debug("Workflow: {} has following usages: {}", projectScopeId, usages.get(projectScopeId).getUsages().stream().map(id -> id.getId()).collect(Collectors.joining( ", " )));
	}

	@Override
	public Set<ArtifactIdentifier> getUsages(String projectScopeId) {
		if (usages.containsKey(projectScopeId)) {
			return usages.get(projectScopeId).getUsages();
		} else
			return Collections.emptySet();
		
	}

	@Override
	public void removeScope(String projectScopeId) {
		usages.remove(projectScopeId);
		log.info("Removed scope "+projectScopeId );
	}

	@Override
	public Set<String> getAllScopeIdentifier() {
		return usages.keySet();
	}

}
