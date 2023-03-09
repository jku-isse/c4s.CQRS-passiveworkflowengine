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
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;

public class ProgressPusher implements IProgressObserver {

	private List<ProgressEntry> entries = new LinkedList<>();
	private static int maxEntries = 1000;
	private static int batchTruncationSize = 100;
	private ITimeStampProvider timeProvider;
	private Map<Integer, Map.Entry<UI, ListDataProvider<ProgressEntry>>> views = new HashMap<>();
	
	public ProgressPusher(ITimeStampProvider timeProvider) {
		this.timeProvider = timeProvider;
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
		boolean refreshAll = checkSizeAndTruncated();
		entry.completeEntry(this, timeProvider.getLastChangeTimeStamp());
		entries.add(entry);
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

	private boolean checkSizeAndTruncated() {
		if (entries.size() > (maxEntries+batchTruncationSize)) {
			entries.subList(0,  batchTruncationSize).clear();
			return true;
		}
		return false;
	}
}
