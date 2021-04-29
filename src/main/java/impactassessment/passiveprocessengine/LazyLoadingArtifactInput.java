package impactassessment.passiveprocessengine;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.instance.ArtifactIO;
import passiveprocessengine.instance.ArtifactInput;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class LazyLoadingArtifactInput extends ArtifactInput {

	private static final long serialVersionUID = 1L;
	private Set<ArtifactIdentifier> ai = new LinkedHashSet<>();
	private transient IArtifactRegistry reg;
	private String wfi;

	
	@SuppressWarnings("deprecation")
	public LazyLoadingArtifactInput(Set<ArtifactIdentifier> ai, IArtifactRegistry reg, String wfi, String role) {
		this.ai.addAll(ai);
		this.reg = reg;
		this.wfi = wfi;
		super.setRole(role);
	}

	@SuppressWarnings("deprecation")
	public LazyLoadingArtifactInput(ArtifactIdentifier ai, IArtifactRegistry reg, String wfi, String role) {
		this.ai.add(ai);
		this.reg = reg;
		this.wfi = wfi;
		super.setRole(role);
	}
	
	
	
	@Override
	public Set<IArtifact> getArtifacts() {
		Set<IArtifact> artifacts = super.getArtifacts();
		if (artifacts.size() == 0) {
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
		return "LazyLoadingArtifactInput [" + ai + "]";
	}

	public static LazyLoadingArtifactInput generateFrom(ArtifactIO io, IArtifactRegistry reg, String wfi) {
		return new LazyLoadingArtifactInput(io.getArtifacts().stream().map(IArtifact::getArtifactIdentifier).collect(Collectors.toSet()), reg, wfi, io.getRole());
	}
	
	public void reinjectRegistry(IArtifactRegistry reg) {
		this.reg = reg;
	}

	@Override
	public boolean removeArtifact(IArtifact a) {
		super.removeArtifact(a);
		return ai.remove(a.getArtifactIdentifier());
	}

}
