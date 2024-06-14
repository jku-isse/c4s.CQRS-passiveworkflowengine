package at.jku.isse.passiveprocessengine.frontend.ui.monitoring;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ListDataProvider;

import at.jku.isse.designspace.artifactconnector.core.monitoring.IProgressObserver;
import at.jku.isse.designspace.artifactconnector.core.monitoring.ProgressEntry;
import at.jku.isse.passiveprocessengine.frontend.monitoring.ProgressObserver;
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;

public class ProgressPusher extends ProgressObserver {


	private Map<Integer, Map.Entry<UI, ListDataProvider<ProgressEntry>>> views = new HashMap<>();
	
	public ProgressPusher(ITimeStampProvider timeProvider) {
		super(timeProvider);
	}
	
	
    public ListDataProvider<ProgressEntry> add(UI ui) {
    	ListDataProvider<ProgressEntry> dataProvider = new ListDataProvider<>(entries);
    	views.put(ui.getUIId(), new AbstractMap.SimpleEntry<>(ui, dataProvider));    	
    	return dataProvider;
    }

    public void remove(int id) {
        views.remove(id);
    }
	
	@Override
	public void dispatchNewEntry(ProgressEntry entry) {
		super.dispatchNewEntry(entry);		
		views.values().stream()
		.forEach(tuple -> {
			if (tuple.getKey() != null) {
				tuple.getKey().access(() -> {
					//if (refreshAll) 
						tuple.getValue().refreshAll();
					// else
					//	tuple.getValue().refreshItem(entry);
				} );
			}});
	}

	@Override
	public void updatedEntry(ProgressEntry entry) {
		views.values().stream()
		.forEach(tuple -> {
			if (tuple.getKey() != null) {
				tuple.getKey().access(() -> tuple.getValue().refreshAll());
			}});
	}

}
