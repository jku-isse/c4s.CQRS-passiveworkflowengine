package impactassessment.artifactconnector.demo;

import artifactapi.ArtifactIdentifier;
import artifactapi.ArtifactType;

public class DemoRequirement extends DemoArtifact {

	public enum propKeys { status, assessment, type };
	public enum statusValues { open, inprogress, completed };
	public enum assessmentValues { na , inpreparation, done };
	public enum typeValues { highlevel, lowlevel };
	
	public DemoRequirement(DemoService ds, String id) {
		super(ds, id);
		this.ai = new ArtifactIdentifier(id, type.getId());
		this.properties.put(propKeys.status.toString() , statusValues.open.toString());
		this.properties.put(propKeys.assessment.toString() , assessmentValues.na.toString());
		this.properties.put(propKeys.type.toString(), typeValues.lowlevel.toString());
	}

	public static ArtifactType type = new ArtifactType(DemoRequirement.class.getSimpleName());

	@Override
	public ArtifactIdentifier getArtifactIdentifier() {
		return ai;
	}

}
