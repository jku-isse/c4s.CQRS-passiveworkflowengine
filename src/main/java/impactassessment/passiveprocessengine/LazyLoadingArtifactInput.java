package impactassessment.passiveprocessengine;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
	private Set<ArtifactIdentifier> ai;
	private transient IArtifactRegistry reg;
	private String wfi;

	
	@SuppressWarnings("deprecation")
	public LazyLoadingArtifactInput(Set<ArtifactIdentifier> ai, IArtifactRegistry reg, String wfi, ArtifactType type, String role) {
		this.ai = ai;
		this.reg = reg;
		this.wfi = wfi;
		super.setArtifactType(type);
		super.setRole(role);
	}
	
	
	
	@Override
	public Set<IArtifact> getArtifacts() {
		Set<IArtifact> art = super.getArtifacts();
		if (art == null) {
			for (ArtifactIdentifier aId : ai) {
				Optional<IArtifact> artOpt = reg.get(aId, wfi);
				if (artOpt.isPresent()) {
					super.addOrReplaceArtifact(artOpt.get());
				} else {
					log.warn("Could not load artifact from registry:" + ai);
				}
			}
			return super.getArtifacts();
		} else 
			return art;
	}



	@Override
	public String toString() {
		return "LazyLoadingArtifactInput [" + ai + "]";
	}

	public static LazyLoadingArtifactInput generateFrom(ArtifactIO io, IArtifactRegistry reg, String wfi) {
		return new LazyLoadingArtifactInput(io.getArtifacts().stream().map(IArtifact::getArtifactIdentifier).collect(Collectors.toSet()), reg, wfi, io.getArtifactType(), io.getRole());
	}
	
	public void reinjectRegistry(IArtifactRegistry reg) {
		this.reg = reg;
	}

	@Override
	protected void setContainer(IWorkflowTask wt) {
		// TODO setId !!!
	}
}
