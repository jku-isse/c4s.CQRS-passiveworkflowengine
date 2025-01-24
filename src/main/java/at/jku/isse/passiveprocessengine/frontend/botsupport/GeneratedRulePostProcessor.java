package at.jku.isse.passiveprocessengine.frontend.botsupport;

import lombok.Getter;
import lombok.NonNull;


public class GeneratedRulePostProcessor {
	
	@Getter
	private String processedRule;
	
	private GeneratedRulePostProcessor (String rawRule) {
		this.processedRule = rawRule;
	}
	
	public static GeneratedRulePostProcessor init(@NonNull String rawRule) {		
		var processor = new GeneratedRulePostProcessor(rawRule);		
		processor.replaceDomainspecificProperties();
		processor.replaceARLspecifics();
		return processor;
	}
	
	public void replaceDomainspecificProperties() {
		processedRule = processedRule.replace("workItemType", "externalType");		
	}
	
	public void replaceARLspecifics() {
		processedRule = processedRule.replace("oclIsTypeOf", "isTypeOf");
		processedRule = processedRule.replace("oclAsType", "asType");
		processedRule = processedRule.replace("oclIsKindOf", "isKindOf");
		processedRule = processedRule.replace("oclIsUndefined()", "isDefined() = false");		
	}
	
	public void augmentWithTypes(@NonNull String rawRule) {
		// check if there are type mismatches: i.e., property exist in a subtype
		// find a prior collection operator: select, collect, or forall, reject
		// determine which type is there, obtain the subtype, check which ones has the problematic property
		// if multiple compare against context
		// insert type case
	}
	
}
