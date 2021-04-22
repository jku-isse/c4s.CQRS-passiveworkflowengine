package impactassessment.passiveprocessengine;

import java.util.Optional;

import artifactapi.ArtifactIdentifier;
import artifactapi.ArtifactType;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.instance.ArtifactIO;
import passiveprocessengine.instance.ArtifactInput;

@Slf4j
public class LazyLoadingArtifactInput extends ArtifactInput {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArtifactIdentifier ai;
	private transient IArtifactRegistry reg;
	private String wfi;

	
	@SuppressWarnings("deprecation")
	public LazyLoadingArtifactInput(ArtifactIdentifier ai, IArtifactRegistry reg, String wfi, ArtifactType type, String role) {
		this.ai = ai;
		this.reg = reg;
		this.wfi = wfi;
		super.setArtifactType(type);
		super.setRole(role);
	}
	
	
	
	@Override
	public IArtifact getArtifact() {
		IArtifact art = super.getArtifact(); 
		if (art == null) {
			Optional<IArtifact> artOpt = reg.get(ai, wfi);
			if (artOpt.isPresent()) {
				super.setArtifact(artOpt.get());
				return artOpt.get();
			} else {
				log.warn("Could not load artifact from registry:"+ai);
				return null;
			}
		} else 
			return art;
	}



	@Override
	public String toString() {
		return "LazyLoadingArtifactInput [" + ai + "]";
	}

	public static LazyLoadingArtifactInput generateFrom(ArtifactIO io, IArtifactRegistry reg, String wfi) {
		return new LazyLoadingArtifactInput(io.getArtifact().getArtifactIdentifier(), reg, wfi, io.getArtifactType(), io.getRole());
	}
	
	public void reinjectRegistry(IArtifactRegistry reg) {
		this.reg = reg;
	}

}
