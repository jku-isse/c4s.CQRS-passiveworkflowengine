package impactassessment.artifactconnector.demo;

import artifactapi.ArtifactIdentifier;
import artifactapi.ArtifactType;

public class DemoTestCase extends DemoArtifact {

	public DemoTestCase(DemoService ds, String id) {
		super(ds, id);
		this.ai = new ArtifactIdentifier(id, type.getId());
	}

	public static ArtifactType type = new ArtifactType(DemoTestCase.class.getSimpleName());

	@Override
	public ArtifactIdentifier getArtifactIdentifier() {
		return ai;
	}

}
