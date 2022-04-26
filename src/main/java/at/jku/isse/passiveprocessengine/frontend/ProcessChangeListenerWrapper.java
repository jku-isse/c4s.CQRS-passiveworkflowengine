package at.jku.isse.passiveprocessengine.frontend;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;

import artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.artifacts.LazyLoadingListener;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessChangeListenerWrapper extends ProcessInstanceChangeProcessor{

	AtomicInteger counter = new AtomicInteger(0);
	
	Set<ProcessInstance> updatedInstances = Collections.synchronizedSet(new HashSet<>());
	Set<ArtifactIdentifier> lazyLoaded = new HashSet<>();
	
	IFrontendPusher uiUpdater;
	//LazyLoadingListener lazyLoader;
	ArtifactResolver resolver;
	Workspace ws;
	
	public ProcessChangeListenerWrapper(Workspace ws, IFrontendPusher uiUpdater, ArtifactResolver resolver) {
		super(ws);
		this.ws = ws;
		this.uiUpdater = uiUpdater;
		//this.lazyLoader = lazyLoader;
		this.resolver = resolver;
	}

	@Override
	public void handleUpdated(List<Operation> operations) {
		counter.incrementAndGet();
		// get lazyloading
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
		
		updatedInstances.addAll(super.handleUpdates(operations));
		fetchLazyLoaded();
		int current = counter.decrementAndGet();
		if (current == 0 && updatedInstances.size() > 0) {
			//all cascading updates have settled, lets signal update to
			uiUpdater.update(new HashSet<>(updatedInstances));
			updatedInstances.clear();
		}
	}

	private void fetchLazyLoaded() {
		Set<ArtifactIdentifier> ais = getLazyLoadedAndReset();
		while (!ais.isEmpty()) {
			ais.stream()
			.forEach(artId -> {
				try {
					log.debug("Trying to fetch lazyloaded artifact: "+artId.toString());
					Instance inst =  resolver.get(artId);
				} catch (ProcessException e) {
					log.warn("Could not fetch lazyloaded artifact: "+artId.toString()+" due to: "+e.getMessage());
				}
			});
			//ws.concludeTransaction();
			//ws.commit();
			ais = getLazyLoadedAndReset();
		}
		
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
	
	private Set<ArtifactIdentifier> getLazyLoadedAndReset() {
		Set<ArtifactIdentifier> copy = new HashSet<>(lazyLoaded);
		lazyLoaded.clear();
		return copy;
	}
	
}
