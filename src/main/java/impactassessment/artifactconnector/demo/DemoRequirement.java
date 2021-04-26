package impactassessment.artifactconnector.demo;

import artifactapi.ArtifactIdentifier;
import artifactapi.ArtifactType;

public class DemoRequirement extends DemoArtifact {

	public static ArtifactType type = new ArtifactType(DemoRequirement.class.getSimpleName());

	@Override
	public ArtifactIdentifier getArtifactIdentifier() {
		return ai;
	}

}
