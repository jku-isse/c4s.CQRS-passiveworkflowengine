package impactassessment.model.workflowmodel;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class ArtifactType extends IdentifiableObject{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6672658944580547694L;
	//private String artifactType;

	@Deprecated
	public ArtifactType() {
		super();
	}
	
	public ArtifactType(String artifactType) {
		super(artifactType);
		//this.artifactType = artifactType;
	}

	public String getArtifactType() {
		return super.getId();
		//return artifactType;
	}

	public void setArtifactType(String artifactType) {
		//this.artifactType = artifactType;
		super.id = artifactType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		//result = prime * result + ((artifactType == null) ? 0 : artifactType.hashCode());
		result = prime * result + ((super.id == null) ? 0 : super.id.hashCode());
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
		ArtifactType other = (ArtifactType) obj;
		if (super.id == null) {
			if (other.getId() != null)
				return false;
		} else if (!super.id.equals(other.getId()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getId();
	}
	
}
