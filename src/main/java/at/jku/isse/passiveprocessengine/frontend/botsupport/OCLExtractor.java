package at.jku.isse.passiveprocessengine.frontend.botsupport;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OCLExtractor {

	private final String rawText;
	private String extractedOCL = null;
	
	public String extractOCLorNull() {
		return tryExtractFromSelf(extractOCLScope(rawText));
    }
	
	protected String extractOCLScope(String rawText) {
		int postOfTrippleBegin = rawText.indexOf("```");
		int beginScope = Math.max(postOfTrippleBegin, 0);
		int posOfTrippleTick = rawText.lastIndexOf("```");
		int endScope = posOfTrippleTick > 0 ? posOfTrippleTick : rawText.length();
		return rawText.substring(beginScope+3, endScope);
	}
	
	protected String tryExtractFromSelf(String rawText) {						
    	int pos = rawText.indexOf("self");    	
    	if (pos >= 0) {    		    		    		
    		extractedOCL = rawText.substring(pos).trim();    		 		
    		return extractedOCL;
    	} else
    		return tryRepairMissingSelf(rawText);    	
	}
	
	protected String tryRepairMissingSelf(String rawText) {
		// try find invariant name and ':'
		int posOfInv = rawText.indexOf("inv");
		if (posOfInv > -1) {
			rawText = rawText.substring(posOfInv+3);
			int posOfColon = rawText.indexOf(":");
			if (posOfColon > -1) {
				rawText = rawText.substring(posOfColon+1);
				return "self."+rawText.trim();				
			} else {
				// lets wing it:
				return "self."+rawText.trim();
			}			
		} else return null; // neither inv nor self found, giving up
		
	}
}
