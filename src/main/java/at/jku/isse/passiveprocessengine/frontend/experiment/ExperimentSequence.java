package at.jku.isse.passiveprocessengine.frontend.experiment;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;

@Data
public class ExperimentSequence {

	final String participantId;
	List<TaskInfo> sequence = new LinkedList<>();
	
	
	@Data
	public static class TaskInfo {
		final String processId;
		final String inputParam;
		final String inputId;
		final String artifactType;
		final String idType;
		final String repairSupport;
		final String inputUrl;
	}
	
}


