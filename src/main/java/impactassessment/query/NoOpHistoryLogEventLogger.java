package impactassessment.query;

import java.util.List;

public class NoOpHistoryLogEventLogger implements IHistoryLogEventLogger {

	@Override
	public void log(List<String> entryAsJson) {
		// no op
	}

}
