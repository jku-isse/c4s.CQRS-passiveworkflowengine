package impactassessment.query;

import java.util.List;

public class ConsoleHistoryLogEventLogger implements IHistoryLogEventLogger {

	@Override
	public void log(List<String> entriesAsJson) {
		entriesAsJson.forEach(entryAsJson -> System.out.println(entryAsJson));
	}

}
