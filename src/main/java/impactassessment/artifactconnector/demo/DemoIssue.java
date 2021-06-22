package impactassessment.artifactconnector.demo;

import artifactapi.ArtifactIdentifier;
import artifactapi.ArtifactType;

public class DemoIssue extends DemoArtifact {

	public DemoIssue(DemoService ds, String id) {
		super(ds, id);
		this.ai = new ArtifactIdentifier(id, type.getId());
	}

	public static ArtifactType type = new ArtifactType(DemoIssue.class.getSimpleName());

	@Override
	public ArtifactIdentifier getArtifactIdentifier() {
		return ai;
	}

}
