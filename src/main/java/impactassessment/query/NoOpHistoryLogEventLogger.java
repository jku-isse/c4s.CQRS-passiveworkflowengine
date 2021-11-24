package impactassessment.query;

public class NoOpHistoryLogEventLogger implements IHistoryLogEventLogger {

	@Override
	public void log(String entryAsJson) {
		// no op
	}

}
