package at.jku.isse.passiveprocessengine.frontend.ui.components;

import org.apache.jena.ontapi.model.OntObject;

import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RuleEnabledResolver;
import lombok.AllArgsConstructor;

@AllArgsConstructor 
public class DefaultProcessRepairTreeFilter extends RepairTreeFilter {

	final RuleEnabledResolver abstractionMapper;
	
	@Override
	public boolean compliesTo(RepairAction ra) {
		// lets not suggest any repairs that cannot be navigated to in an external tool. 
		if (ra.getElement() == null) return false;
		OntObject artifact = (OntObject) ra.getElement();
		PPEInstance ppeArt = (PPEInstance) abstractionMapper.resolveToRDFElement(artifact);
		
		if (!ppeArt.getInstanceType().hasPropertyType(CoreTypeFactory.URL_URI) 
				|| ppeArt.getTypedProperty(CoreTypeFactory.URL_URI, String.class) == null) { 
			return false;
		} else
			return ra.getProperty() != null 
				//&& !ra.getProperty().equalsIgnoreCase("workItemType") // now done via metaproperties
				&& !ra.getProperty().startsWith("out_") // no change to input or output --> WE do suggest as an info that it needs to come from somewhere else, other step
				&& !ra.getProperty().startsWith("in_")
				&& !ra.getProperty().equalsIgnoreCase("name") // typically used to describe key or id outside of designspace
				&& abstractionMapper.isPropertyRepairable(ppeArt.getInstanceType(), ra.getProperty())
				; 
	
	}
	
}