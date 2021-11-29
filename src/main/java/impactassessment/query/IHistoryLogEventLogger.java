package impactassessment.query;

import java.util.List;

public interface IHistoryLogEventLogger {

	public void log(List<String> entriesAsJson);
}
