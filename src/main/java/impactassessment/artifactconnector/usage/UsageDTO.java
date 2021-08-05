package impactassessment.artifactconnector.usage;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;

import artifactapi.ArtifactIdentifier;

@Entity(name = "ArtUsage")
@Table(name = "ArtUsage")
public class UsageDTO {
		
	
	@Id 
	String projectScopeId;
	
	@ElementCollection(fetch = FetchType.EAGER)	
	Set<ArtifactIdentifier> ais = new HashSet<>();

	public UsageDTO() {};
	
	public UsageDTO(String projectScopeId) {
		this.projectScopeId = projectScopeId;
	};
	
	public String getProjectScopeId() {
		return projectScopeId;
	}

	public void setProjectScopeId(String projectScopeId) {
		this.projectScopeId = projectScopeId;
	}

	public Set<ArtifactIdentifier> getUsages() {
		return ais;
	}
	
	
	
}
