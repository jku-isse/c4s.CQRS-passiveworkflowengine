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
import passiveprocessengine.instance.ArtifactOutput;

@Slf4j
public class LazyLoadingArtifactOutput extends ArtifactOutput {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Set<ArtifactIdentifier> ai;
	private transient IArtifactRegistry reg;
	private String wfi;
	
	@SuppressWarnings("deprecation")
	public LazyLoadingArtifactOutput(Set<ArtifactIdentifier> ai, IArtifactRegistry reg, String wfi, ArtifactType type, String role) {
		this.ai = ai;
		this.reg = reg;
		this.wfi = wfi;
		super.setArtifactType(type);
		super.setRole(role);
	}
	
	
	
	@Override
	public Set<IArtifact> getArtifacts() {
		Set<IArtifact> artifacts = super.getArtifacts();
		if (artifacts == null) {
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
			return artifacts;
	}



	@Override
	public String toString() {
		return "LazyLoadingArtifactOutput [" + ai + "]";
	}

	public static LazyLoadingArtifactOutput generateFrom(ArtifactIO io, IArtifactRegistry reg, String wfi) {
		return new LazyLoadingArtifactOutput(io.getArtifacts().stream().map(IArtifact::getArtifactIdentifier).collect(Collectors.toSet()), reg, wfi, io.getArtifactType(), io.getRole());
	}
	
	public void reinjectRegistry(IArtifactRegistry reg) {
		this.reg = reg;
	}

	@Override
	protected void setContainer(IWorkflowTask wt) {
		// TODO setId !!!
	}
}
