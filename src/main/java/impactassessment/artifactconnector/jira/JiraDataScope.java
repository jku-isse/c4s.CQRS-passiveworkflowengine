package impactassessment.artifactconnector.jira;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Optional;

import artifactapi.jira.IJiraArtifact;
import c4s.jiralightconnector.IssueAgent;

public class JiraDataScope implements IJiraService{

	private IJiraService origin;
	private String scopeId;
	private HashMap<String, WeakReference<Object>> cache = new HashMap<String, WeakReference<Object>>();
	
	JiraDataScope(String scopeId, IJiraService origin) {
		this.scopeId = scopeId;
		this.origin = origin;
	}
		
	@Override
	public Optional<IJiraArtifact> getIssue(String id, String workflowId) {
		WeakReference<Object> ref = cache.getOrDefault("JiraIssue"+id, new WeakReference<Object>(null));
		if (ref.get() == null) {
			Optional<IJiraArtifact> opt = origin.getIssue(id, workflowId);
			if (opt.isPresent()) {
				cache.put("JiraIssue"+id, new WeakReference<Object>(opt.get()));
				return opt;
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.ofNullable((IJiraArtifact)ref.get());
		}
	}
	
	public IJiraArtifact replaceWithUpdate(IssueAgent issue) {
		IJiraArtifact art = new JiraArtifact(issue.getIssue(), this);
		cache.put("JiraIssue"+issue.getKey(), new WeakReference<Object>(art));
		return art;
	}

	@Override
	public Optional<IJiraArtifact> getIssue(String key) {
		return getIssue(key, scopeId);
	}

	public IJiraService getOrigin() {
		return origin;
	}

	public void setOrigin(IJiraService origin) {
		this.origin = origin;
	}

	public String getScopeId() {
		return scopeId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scopeId == null) ? 0 : scopeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JiraDataScope other = (JiraDataScope) obj;
		if (scopeId == null) {
			if (other.scopeId != null)
				return false;
		} else if (!scopeId.equals(other.scopeId))
			return false;
		return true;
	}
 
}
