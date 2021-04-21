package impactassessment.artifactconnector;

import artifactapi.IArtifact;
import artifactapi.ResourceLink;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jira.IJiraArtifact;

public class ResourceLinkUtil {

	public static ResourceLink enhancedLinkTitle(IArtifact art, String titleExtension) {
		ResourceLink rl = art.convertToResourceLink();
		if (rl != null) {
			rl.setTitle(rl.getTitle()+" "+titleExtension);
		}
		return rl;
	}
	
	public static ResourceLink replacedLinkTitle(IArtifact art, String title) {
		ResourceLink rl = art.convertToResourceLink();
		if (rl != null) {
			rl.setTitle(title);
		}
		return rl;
	}
	
	public static ResourceLink toJiraLink(IJiraArtifact art) {
		return enhancedLinkTitle(art, art.getSummary());
	}
	
	public static ResourceLink toJamaLink(IJamaArtifact art) {
		return enhancedLinkTitle(art, art.getName());
	}
}
