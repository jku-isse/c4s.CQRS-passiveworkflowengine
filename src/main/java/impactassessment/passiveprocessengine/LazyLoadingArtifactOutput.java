package impactassessment.passiveprocessengine;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.instance.ArtifactIO;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.WorkflowChangeEvent;
import passiveprocessengine.instance.WorkflowChangeEvent.ChangeType;
import passiveprocessengine.instance.events.TaskArtifactEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
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
	public List<WorkflowChangeEvent> addOrReplaceArtifact(IArtifact artifact, WorkflowChangeEvent wce) {		
		this.ai.add(artifact.getArtifactIdentifier()); //to keep identifiers synced with artifacts
		return super.addOrReplaceArtifact(artifact, wce); //this will add container task in events
	}
	
	public List<WorkflowChangeEvent> addOrReplaceArtifact(ArtifactIdentifier ai, WorkflowChangeEvent wce) {
		boolean added = this.ai.add(ai);
		if (added) {
			List<WorkflowChangeEvent> changes = new LinkedList<>();
			WorkflowChangeEvent newWCE = new TaskArtifactEvent(ChangeType.NEW_OUTPUT, container, Set.of(ai), wce);
			changes.add(newWCE);
			newWCE.getCascadingEffect().addAll(container.triggerUponAddedOrRemovedOutput(wce));
			return changes;
		}
		else
			return Collections.emptyList();
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
					if (!findById(artifacts, aId)) {
						Optional<IArtifact> artOpt = reg.get(aId, wfi);
						if (artOpt.isPresent()) {
							super.artifacts.add(artOpt.get()); // we have signaled the adding already earlier, now its just to sync content
							//super.addOrReplaceArtifact(artOpt.get());
						} else {
							log.warn("Could not load artifact from registry:" + ai);
						}
					}
				}
			}
			return super.getArtifacts(); // just to be on the save side, if super.getArtifacts would return an immutable list, any change due to sync would not be visible in initial fetched set.
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
	public List<WorkflowChangeEvent> removeArtifact(IArtifact a, WorkflowChangeEvent wce) {
		//check if lazyoading art already known in parent artio, if not then it will not have an effect and we will have to trigger it		
		boolean removed = ai.remove(a.getArtifactIdentifier());
		if (removed) {
		if (super.containsArtifactByIdentifier(a.getArtifactIdentifier()))
			return super.removeArtifact(a, wce);
		else {
			List<WorkflowChangeEvent> changes = new LinkedList<>();
			WorkflowChangeEvent newWCE = new TaskArtifactEvent(ChangeType.OUTPUT_DELETED, container,Set.of(a.getArtifactIdentifier()), wce );
			changes.add(newWCE);
			newWCE.getCascadingEffect().addAll(container.triggerUponAddedOrRemovedOutput(newWCE));
			return changes;
		}
		} else { // even we dont know about it
			return Collections.emptyList();
		}
	}

	@Override
	public Set<ArtifactIdentifier> getArtifactIdentifiers() {
		return ai;
	}

	@Override
	public boolean containsArtifactByIdentifier(ArtifactIdentifier aid) {		
		return ai.contains(aid);
	}

	@Override
	public List<WorkflowChangeEvent> removeArtifactsById(Set<ArtifactIdentifier> ais, WorkflowChangeEvent wce) {
		// worst case: some art is only known here, others is known also to super						
		Set<ArtifactIdentifier> inSuper = ais.stream().filter(ai -> super.containsArtifactByIdentifier(ai)).collect(Collectors.toSet());		
		//first remove all local ones
		Set<ArtifactIdentifier> locallyRemoved = new HashSet<>();
		long localRemoveCount = ais.stream().map(aid -> { 
					if (ai.remove(aid)) {
						locallyRemoved.add(aid);
						return true;
					}
					return false;
				})
				.filter(b -> true).count();				
		
		List<WorkflowChangeEvent> changes = new LinkedList<>();
		if (locallyRemoved.size() > 0) {
			changes.add(new TaskArtifactEvent(ChangeType.OUTPUT_DELETED, container, locallyRemoved, wce));
		}
		// then remove those from super
		
		
		List<WorkflowChangeEvent> superChanges = super.removeArtifactsById(inSuper, wce);
		if (superChanges.isEmpty() && localRemoveCount > 0 ) { //no ai was known there but some here so we need to trigger
			changes.addAll(container.triggerUponAddedOrRemovedOutput(wce));
		} else { //super has already triggered change propagation incl local task listed, or local changes were also zero, then equally fine to pass on changes
			changes.addAll(superChanges);
		}
		return changes;
	}

	@Override
	public List<WorkflowChangeEvent> addNewArtifactsFromArtifactIO(ArtifactIO aio, WorkflowChangeEvent wce) {
		Set<ArtifactIdentifier> newCount = aio.getArtifactIdentifiers().stream()
				.filter(aid -> !ai.contains(aid))
				.map(aid -> { 
					ai.add(aid);
					return aid;
				})
				.collect(Collectors.toSet());
		if (newCount.size() > 0) {
			List<WorkflowChangeEvent> changes = new LinkedList<>();
			WorkflowChangeEvent newWCE = new TaskArtifactEvent(ChangeType.NEW_OUTPUT, container, newCount, wce);
			changes.add(newWCE);
			newWCE.getCascadingEffect().addAll(container.triggerUponAddedOrRemovedOutput(newWCE));
			return changes;
		} else
			return Collections.emptyList();					
	}
		
}
