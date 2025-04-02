package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OCLExtractor {

	private static final String TRIPLETICK = "```";
	private final String rawText;
	//private String extractedOCL = null;
	
	public String extractOCLorEmpty() {
		return tryExtractFromSelf(stripAwayOCLMarker(extractTripleTickScope(rawText.trim())));
    }
	
	protected String extractTripleTickScope(String rawText) {		
		var regions = findRegionsOfTrippleTick(rawText);
		if (regions.isEmpty()) {
			var singleTriple = rawText.indexOf(TRIPLETICK);
			if (singleTriple >= 0) {
				return rawText.substring(singleTriple+3);
			}
			return rawText;
		}
		// potentially multiple ones, lets use the last one.
		var region = regions.get(regions.size()-1);
		
		return rawText.substring(region.getKey()+3, region.getValue());
//		int posOfTrippleBegin = rawText.indexOf(TRIPLETICK);
//		int beginScope = posOfTrippleBegin >= 0 ? posOfTrippleBegin+3 : 0;
//		int posOfTrippleTick = rawText.lastIndexOf(TRIPLETICK);
//		int endScope = posOfTrippleTick > 0 ? posOfTrippleTick : rawText.length();
//		return rawText.substring(beginScope, endScope);
	}
	
	protected String stripAwayOCLMarker(String rawText) {
		if (rawText.isEmpty()) return rawText;
		var text = rawText.trim();
		if (text.startsWith("ocl")) {
			return text.substring(3);
		}
		return text;
	}
	
	protected String tryExtractFromSelf(String rawText) {						
    	if (rawText.isEmpty()) return rawText;
		int pos = rawText.indexOf("self");    	
    	if (pos >= 0) {    		    		    		
    		return rawText.substring(pos).trim();
//    		extractedOCL = rawText.substring(pos).trim();    		 		
//    		return extractedOCL;
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
		} else {
			// try to wing it by just adding self infront
			return "self."+rawText.trim();
		}
		
	}
	
	protected List<Entry<Integer, Integer>> findRegionsOfTrippleTick(String rawText) {
		var regionList = new ArrayList<Entry<Integer, Integer>>();
		int posOfTriple = rawText.indexOf(TRIPLETICK);
		while (posOfTriple >= 0 && posOfTriple+1 < rawText.length()) {
			int posOfNextTriple = rawText.indexOf(TRIPLETICK, posOfTriple+1);
			if (posOfNextTriple > posOfTriple) { // found a region
				regionList.add(new AbstractMap.SimpleEntry<>(posOfTriple, posOfNextTriple));
				// lets look for the next starting tripletick
				if (posOfNextTriple+1 < rawText.length()) {
					posOfTriple = rawText.indexOf(TRIPLETICK, posOfNextTriple+1);
				} else { // no more text to check
					break;
				}
			} else { 
				// non matched starting triple tick; lets just use end of raw text unless there is no raw text left
				if (posOfTriple+3<rawText.length()) {
					regionList.add(new AbstractMap.SimpleEntry<>(posOfTriple, rawText.length()));
				}
				break;
			}
		}
		
		return regionList;
	}
}
