package at.jku.isse.passiveprocessengine.frontend.registry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import at.jku.isse.designspace.stagesexport.TransformDeployResult;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry.ProcessDeployResult;

@Component
public class DeployResultPersistence {
	
	private ProcessDeployResult lastResult = null;

	private Map<String, ProcessDeployResult> results = new HashMap<>();
	
	public void setLastResult(ProcessDeployResult lastResult) {
		if (lastResult != null) {
			this.lastResult = lastResult;
			if (lastResult.getProcDef() != null) {
				this.results.put(lastResult.getProcDef().getName(), lastResult);
			}
		}
	}
	
	public ProcessDeployResult getLastResult() {
		return lastResult;
	}
	
	public ProcessDeployResult getResult(String id) {
		return results.get(id);
	}
	
	
}
