package at.jku.isse.passiveprocessengine.frontend;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ConditionChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.IOMappingConsistencyCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.OutputChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.PrematureStepTriggerCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ProcessScopedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.QAConstraintChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessChangeListenerWrapper extends ProcessInstanceChangeProcessor{

	AtomicInteger counter = new AtomicInteger(0);
	
	Set<ProcessInstance> updatedInstances = Collections.synchronizedSet(new HashSet<>());
	Set<ArtifactIdentifier> lazyLoaded = Collections.synchronizedSet(new HashSet<>());
	
	IFrontendPusher uiUpdater;
	//LazyLoadingListener lazyLoader;
	ArtifactResolver resolver;
	Workspace ws;
	
	public ProcessChangeListenerWrapper(Workspace ws, IFrontendPusher uiUpdater, ArtifactResolver resolver, EventDistributor eventDistributor) {
		super(ws, eventDistributor);
		this.ws = ws;
		this.uiUpdater = uiUpdater;
		//this.lazyLoader = lazyLoader;
		this.resolver = resolver;
	}

	@Override
	public void handleUpdated(List<Operation> operations) {
		counter.updateAndGet(i -> { return i < 0 ? 1 : i+1; });
//		counter.incrementAndGet();
		// get lazyloading
		operations.stream()
		 .map(operation -> {
			if (operation instanceof PropertyUpdateAdd) {
				return processPropertyUpdateAdd((PropertyUpdateAdd) operation);
			} else return null;
		 })
		 .filter(Objects::nonNull)
		 .distinct()
		 .map(inst -> getArtifactIdentifier(inst))
		 .filter(Objects::nonNull)
		 .forEach(ai -> lazyLoaded.add(ai));
		
		updatedInstances.addAll(super.handleUpdates(operations));
		
		int current = counter.decrementAndGet();
		if (current == 0 && updatedInstances.size() > 0) {
			//all cascading updates have settled, lets signal update to
			uiUpdater.update(new HashSet<>(updatedInstances));
			updatedInstances.clear();
		}
	}
	
	public int resetAndUpdate() {
		// just a quick hack for now 
		// as the counter above sometimes remains at the end at a value > 0, 
		// hence no more updates are done thereafter
		counter.set(0);
		int count = updatedInstances.size();
		if (updatedInstances.size() > 0) {
			uiUpdater.update(new HashSet<>(updatedInstances));
			updatedInstances.clear();
		}
		return count;
	}
	
	private static Supplier<Boolean> falseSupplier = () -> Boolean.FALSE;
	
	private Instance processPropertyUpdateAdd(PropertyUpdateAdd op) {
		if (op.name().endsWith("@rl_ruleScopes") ) {
			//Id addedId = (Id) op.value();
			//Element added = ws.findElement(addedId);
			Element element = ws.findElement(op.elementId());
			if (element instanceof Instance 
					&& element.hasProperty("fullyFetched") 
					&& (      element.getPropertyAsValueOrElse("fullyFetched", falseSupplier) == null 
					         || ((Boolean)element.getPropertyAsValueOrElse("fullyFetched", falseSupplier)) == false     )
			){
				return (Instance) element;
			}
		}
		return null;
	}
	
	@Override
	protected void prepareQueueExecution(List<ProcessScopedCmd> mostRecentQueuedEffects) {
		// check for each queued effect if it undos a previous one: e.g., a constraint fufillment now is unfulfilled, datamapping fulfilled is no unfulfilled
		mostRecentQueuedEffects.stream().forEach(cmd -> {
			if (cmd instanceof QAConstraintChangedCmd) {
				QAConstraintChangedCmd qac = (QAConstraintChangedCmd) cmd;
				String key = qac.getCrule().name()+qac.getStep().getName();
//				if (cmdQueue.containsKey(key)) {
//					QAConstraintChangedCmd qacOld = (QAConstraintChangedCmd)cmdQueue.remove(key); // this cmd now is the revers of the previous one.
//					// just in case
//					if (qacOld.isFulfilled() == qac.isFulfilled()) { // this should never be the case
//						log.error("Duplicate ProcessScopedCmd encountered, reinserting in execution queue: "+cmd);
//						cmdQueue.put(key, cmd);
//					}
//				} else
					cmdQueue.put(key, cmd); //we override the last entry, executing a command again has no effect as we check for a change inside the processstep.
			} else if (cmd instanceof ConditionChangedCmd) {
				ConditionChangedCmd qac = (ConditionChangedCmd) cmd;
				String key = qac.getCondition()+qac.getStep().getName();
				cmdQueue.put(key, cmd);
			} else if (cmd instanceof IOMappingConsistencyCmd) {
				IOMappingConsistencyCmd qac = (IOMappingConsistencyCmd) cmd;
				String key = qac.getCrule().name()+qac.getStep().getName();
				cmdQueue.put(key, cmd);
			} else if (cmd instanceof PrematureStepTriggerCmd) {
				PrematureStepTriggerCmd pac = (PrematureStepTriggerCmd) cmd;
				String key = pac.getScope().getName() + pac.getSd().getName();
				cmdQueue.put(key, cmd);
			} else if (cmd instanceof OutputChangedCmd) {
				OutputChangedCmd occ = (OutputChangedCmd)cmd;
				String key = occ.getStep().getName()+occ.getChange().name();
				cmdQueue.putIfAbsent(key, occ);
			} else {
				log.error("Encountered unknown ProcessScopedCmd, ignoring: "+cmd);
			}
		});
		
		// get lazyloading
		fetchLazyLoaded();
		
	}
	
	protected Set<ProcessInstance> executeCommands() {
		if (lazyLoaded.isEmpty())
			return super.executeCommands(); // then there is no lazy loading necessary anymore, we can execute the commands
		else // we skip this until lazy loading is complete
			return Collections.emptySet();
	}

	private void fetchLazyLoaded() {
		Optional<ArtifactIdentifier> optAI = getLazyLoadedArtifact();
		while (optAI.isPresent()) {
			ArtifactIdentifier art = optAI.get();	
			try {
					log.debug("Trying to fetch lazyloaded artifact: "+art.getId().toString());
					Instance inst =  resolver.get(art);
				} catch (ProcessException e) {
					log.warn("Could not fetch lazyloaded artifact: "+art.getId().toString()+" due to: "+e.getMessage());
				}
			lazyLoaded.remove(art);
			optAI = getLazyLoadedArtifact(); // get the next one.
		}
	}
	
	private ArtifactIdentifier getArtifactIdentifier(Instance inst) {
		// FIXME: very brittle
		return new ArtifactIdentifier(inst.name() , inst.getInstanceType().name());
	}
	
	private Optional<ArtifactIdentifier> getLazyLoadedArtifact() {
		return lazyLoaded.stream().findAny();
	}
	
	
}
