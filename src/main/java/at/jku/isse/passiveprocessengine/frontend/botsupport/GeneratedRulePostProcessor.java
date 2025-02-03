package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
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
		processor.augmentTypeReferences();
		return processor;
	}
	
	public void replaceDomainspecificProperties() {
		processedRule = processedRule.replace("workItemType", "externalType");		
	}
	
	public void replaceARLspecifics() {
		processedRule = processedRule.replace("oclIsTypeOf(", "isTypeOf(");
		processedRule = processedRule.replace("oclAsType(", "asType(");
		processedRule = processedRule.replace("oclIsKindOf(", "isKindOf(");
		processedRule = processedRule.replace("oclIsUndefined()", "isDefined() = false");		
	}
	
	public void augmentTypeReferences() {
		// the methods above require <> around types for ARL/OCLX to work
		augmentTypeReference("asType(", 0);
		augmentTypeReference("isKindOf(", 0);
		augmentTypeReference("isTypeOf(", 0);
	}
	
	// we assume types are always directly provided as String, not via a method/operation call or property access
	private void augmentTypeReference(String typeCall, int searchIndex) {
		int pos = processedRule.indexOf(typeCall, searchIndex);
		if (pos == -1 || pos <= searchIndex) return; // not found or no further found
		Map<Integer, Integer> brackets = generateMatchingBracketPos(processedRule);
		// check if the brackets of this typecall are matching, if so, if there is an opening and closing < > within the call
		var  posTypeBracketBegin = pos + typeCall.length() - 1;
		var closeBracket = brackets.get(posTypeBracketBegin);
		if (closeBracket == null || closeBracket == -1) return; // round bracket mitmatch
		var typeBracketEnd  = processedRule.indexOf(">", posTypeBracketBegin) ;
		if (typeBracketEnd == -1 || typeBracketEnd > closeBracket) { // no > within call
			// insert at end, just before )
			processedRule = processedRule.substring(0, closeBracket) + ">" + processedRule.substring(closeBracket);
		}
		var typeBracketBegin = processedRule.indexOf("<", posTypeBracketBegin) ;
		if (typeBracketBegin == -1 || typeBracketBegin > closeBracket) { // no < within call
			// insert at beginning, just after (
			processedRule = processedRule.substring(0, posTypeBracketBegin+1) + "<" + processedRule.substring(posTypeBracketBegin+1);
		}
		augmentTypeReference(typeCall, pos); // we potentially have extended the string by 2 char, but typeCalls are longer anyway
	}
	
	public void augmentWithTypes(@NonNull String rawRule) {
		// check if there are type mismatches: i.e., property exist in a subtype
		// find a prior collection operator: select, collect, or forall, reject
		// determine which type is there, obtain the subtype, check which ones has the problematic property
		// if multiple compare against context
		// insert type case
	}
	
	final static char skipContent = '\'';
	final static char open = '(';
	final static char close = ')';
	
	/** very primitive checking of brackets  '(' match, at which pos, and ignoring simple ' quotations, stops upon first mismatch
	 * @param toParse
	 * @return positions of corresponding open and close brackets, closing pos of -1 for open brackets encountered until first mismatch
	 */
	protected Map<Integer, Integer> generateMatchingBracketPos(String toParse) {
		// inspiration from: https://stackoverflow.com/questions/20506179/check-if-a-given-string-is-balanced-brackets-string-recursively
		Map<Integer, Integer> startEndPairs = new HashMap<>();
		var symbolStack = new ArrayDeque<Character>();
		var posStack = new ArrayDeque<Integer>();
		boolean isSkippingOn = false; // used as toggle flag when to skip characters
		for (int i = 0; i < toParse.length(); i++) {
			var cur = toParse.charAt(i);
			// we dont consider escaped quotations, nor nested quotations
			if (cur == skipContent) {
				isSkippingOn = !isSkippingOn;
			} else 
			if (!isSkippingOn) {	
				if ( open == cur) {
					symbolStack.addFirst(cur);
					posStack.addFirst(i);
				} else if (close == cur) {
					if (symbolStack.isEmpty()) break;
					if (symbolStack.peek() == open) { // opening bracket matches this closing bracket
						startEndPairs.put(posStack.pop(), i);
						symbolStack.pop();
					} else break;
				}
			}
		}
		while (!posStack.isEmpty()) { // any nonmatched brackets
			startEndPairs.put(posStack.pop(), -1);
		}
		return startEndPairs;
	}
	
}
