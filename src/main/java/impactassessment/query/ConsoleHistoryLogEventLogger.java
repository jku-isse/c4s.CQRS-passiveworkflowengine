package impactassessment.query;

public class ConsoleHistoryLogEventLogger implements IHistoryLogEventLogger {

	@Override
	public void log(String entryAsJson) {
		System.out.println(entryAsJson);
	}

}
