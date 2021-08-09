package impactassessment.passiveprocessengine;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.instance.ArtifactIO;
import passiveprocessengine.instance.ArtifactOutput;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class LazyLoadingArtifactOutput extends ArtifactOutput {

	private static final long serialVersionUID = 1L;
	private Set<ArtifactIdentifier> ai = new LinkedHashSet<>();
	private transient IArtifactRegistry reg;
	private String wfi;
	
	@SuppressWarnings("deprecation")
	public LazyLoadingArtifactOutput(Set<ArtifactIdentifier> ai, IArtifactRegistry reg, String wfi, String role) {
		this.ai.addAll(ai);
		this.reg = reg;
		assert(reg!=null);
		this.wfi = wfi;
		super.setRole(role);
	}

	@SuppressWarnings("deprecation")
	public LazyLoadingArtifactOutput(ArtifactIdentifier ai, IArtifactRegistry reg, String wfi, String role) {
		this.ai.add(ai);
		this.reg = reg;
		assert(reg!=null);
		this.wfi = wfi;
		super.setRole(role);
	}
	
	
	@Override
	public void addOrReplaceArtifact(IArtifact artifact) {
		super.addOrReplaceArtifact(artifact);
		this.ai.add(artifact.getArtifactIdentifier()); //to keep identifiers synced with artifacts
	}
	
	public void addOrReplaceArtifact(ArtifactIdentifier ai) {
		this.ai.add(ai);
	}
	
	@Override
	public Set<IArtifact> getArtifacts() {
		Set<IArtifact> artifacts = super.getArtifacts();
		if (artifacts.size() != ai.size()) { //then there is something to load
			if (reg==null) {
				String parent = /*super.getContainer() != null ? super.getContainer().getId() : */"NOT SET";
				log.warn("Registry ref is null for: "+this.getRole()+ " of "+parent);
			} else {
			for (ArtifactIdentifier aId : ai) {
				Optional<IArtifact> artOpt = reg.get(aId, wfi);
				if (artOpt.isPresent()) {
					super.addOrReplaceArtifact(artOpt.get());
				} else {
					log.warn("Could not load artifact from registry:" + ai);
				}
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
		return new LazyLoadingArtifactOutput(io.getArtifacts().stream().map(IArtifact::getArtifactIdentifier).collect(Collectors.toSet()), reg, wfi, io.getRole());
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
