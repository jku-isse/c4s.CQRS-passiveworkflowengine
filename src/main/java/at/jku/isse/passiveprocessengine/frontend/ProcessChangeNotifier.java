package at.jku.isse.passiveprocessengine.frontend;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import at.jku.isse.artifacteventstreaming.rule.RuleSchemaProvider;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PropertyChange.Update;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessChangeNotifier extends ProcessChangeListenerWrapper{

	AtomicInteger counter = new AtomicInteger(0);		
	IFrontendPusher uiUpdater;
			
	public ProcessChangeNotifier(ProcessContext context, IFrontendPusher uiUpdater, ArtifactResolver resolver, EventDistributor eventDistributor, RuleSchemaProvider ruleSchema) {
		super(context, resolver, eventDistributor, ruleSchema);	
		this.uiUpdater = uiUpdater;	
	}
	
	
	@Override
	public void handleUpdates(Collection<Update> operations) {
		counter.updateAndGet(i -> { return i < 0 ? 1 : i+1; });
		super.handleUpdates(operations);		
		int current = counter.decrementAndGet();
		if (current == 0 && updatedInstances.size() > 0) {
			//all cascading updates have settled, lets signal update to
			uiUpdater.update(new HashSet<>(updatedInstances));
			updatedInstances.clear();
		}
	}
	
	
}
