package at.jku.isse.passiveprocessengine.frontend;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;

import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.frontend.ui.IFrontendPusher;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;

public class ProcessChangeListenerWrapper extends ProcessInstanceChangeProcessor{

	AtomicInteger counter = new AtomicInteger(0);
	
	Set<ProcessInstance> updatedInstances = Collections.synchronizedSet(new HashSet<>());
	
	@Autowired
	IFrontendPusher uiUpdater;
	
	public ProcessChangeListenerWrapper(Workspace ws) {
		super(ws);
		
	}

	@Override
	public void handleUpdated(List<Operation> operations) {
		counter.incrementAndGet();
		updatedInstances.addAll(super.handleUpdates(operations));
		int current = counter.decrementAndGet();
		if (current == 0) {
			//all cascading updates have settled, lets signal update to
			uiUpdater.update(new HashSet<>(updatedInstances));
			updatedInstances.clear();
		}
	}

}
