package at.jku.isse.passiveprocessengine.frontend.botsupport;

import lombok.NonNull;

public class GeneratedRulePostProcessor {

	public void process(@NonNull String rawRule) {
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
