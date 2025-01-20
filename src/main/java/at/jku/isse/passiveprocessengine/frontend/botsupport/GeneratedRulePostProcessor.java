package at.jku.isse.passiveprocessengine.frontend.botsupport;

import lombok.NonNull;


public class GeneratedRulePostProcessor {

	private final String rawRule; 
	
	private GeneratedRulePostProcessor (String rawRule) {
		this.rawRule = rawRule;
	}
	
	public static GeneratedRulePostProcessor init(@NonNull String rawRule) {
		var processor = new GeneratedRulePostProcessor(rawRule);
		processor.replaceDomainspecificProperties();
		
		return processor;
	}
	
	public void replaceDomainspecificProperties() {
		rawRule.replace("workItemType", "externalType");		
	}
	
	public void augmentWithTypes(@NonNull String rawRule) {
		// check if there are type mismatches: i.e., property exist in a subtype
		// find a prior collection operator: select, collect, or forall, reject
		// determine which type is there, obtain the subtype, check which ones has the problematic property
		// if multiple compare against context
		// insert type case
	}
	
}
