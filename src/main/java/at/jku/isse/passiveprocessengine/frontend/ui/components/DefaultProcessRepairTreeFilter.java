package at.jku.isse.passiveprocessengine.frontend.ui.components;

import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.designspace.DesignspaceAbstractionMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor 
public class DefaultProcessRepairTreeFilter extends RepairTreeFilter {

	final DesignspaceAbstractionMapper abstractionMapper;
	
	@Override
	public boolean compliesTo(RepairAction ra) {
		// lets not suggest any repairs that cannot be navigated to in an external tool. 
		if (ra.getElement() == null) return false;
		Instance artifact = (Instance) ra.getElement();
		PPEInstance ppeArt = abstractionMapper.mapDesignSpaceInstanceToProcessDomainInstance((Instance) artifact);
		
		if (!ppeArt.getInstanceType().hasPropertyType(CoreTypeFactory.URL) 
				|| ppeArt.getTypedProperty(CoreTypeFactory.URL, String.class) == null) { 
			return false;
		} else
			return ra.getProperty() != null 
				//&& !ra.getProperty().equalsIgnoreCase("workItemType") // now done via metaproperties
				&& !ra.getProperty().startsWith("out_") // no change to input or output --> WE do suggest as an info that it needs to come from somewhere else, other step
				&& !ra.getProperty().startsWith("in_")
				&& !ra.getProperty().equalsIgnoreCase("name") // typically used to describe key or id outside of designspace
				&& ConsistencyUtils.isPropertyRepairable(artifact.getInstanceType(), ra.getProperty())
				; 
	
	}
	
}