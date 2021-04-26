package impactassessment.artifactconnector.demo;

import artifactapi.ArtifactIdentifier;
import artifactapi.ArtifactType;

public class DemoIssue extends DemoArtifact {

	public static ArtifactType type = new ArtifactType(DemoIssue.class.getSimpleName());

	@Override
	public ArtifactIdentifier getArtifactIdentifier() {
		return ai;
	}

}
