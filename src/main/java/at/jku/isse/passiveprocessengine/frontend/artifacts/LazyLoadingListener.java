package at.jku.isse.passiveprocessengine.frontend.artifacts;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.model.WorkspaceListener;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyLoadingListener implements WorkspaceListener {

	Workspace ws;
	
	Set<ArtifactIdentifier> lazyLoaded = new HashSet<>();
	
	public LazyLoadingListener(Workspace ws) {
		this.ws = ws;

		ws.workspaceListeners.add( this);
	}
	
	public Set<ArtifactIdentifier> getLazyLoadedAndReset() {
		Set<ArtifactIdentifier> copy = new HashSet<>(lazyLoaded);
		lazyLoaded.clear();
		return copy;
	}
	
	@Override
	public void handleUpdated(List<Operation> operations) {
		operations.stream()
		 .map(operation -> {
			Element element = ws.findElement(operation.elementId());
			if (operation instanceof PropertyUpdateAdd) {
				return processPropertyUpdateAdd((PropertyUpdateAdd) operation, element);
			} else return null;
		 })
		 .filter(Objects::nonNull)
		 .distinct()
		 .map(inst -> getArtifactIdentifier(inst))
		 .filter(Objects::nonNull)
		 .forEach(ai -> lazyLoaded.add(ai));
	}

	private Instance processPropertyUpdateAdd(PropertyUpdateAdd op, Element element) {
		if (op.name().endsWith("@rl_ruleScopes") ) {
			//Id addedId = (Id) op.value();
			//Element added = ws.findElement(addedId);
			if (element instanceof Instance 
					&& element.hasProperty("fullyFetched") 
					&& ((Boolean)element.getPropertyAsValueOrElse("fullyFetched", () -> false)) == false) {
				return (Instance) element;
			}
		}
		return null;
	}
	
	private ArtifactIdentifier getArtifactIdentifier(Instance inst) {
		// FIXME: very brittle
		return new ArtifactIdentifier(inst.name() , inst.getInstanceType().name());
	}
}
