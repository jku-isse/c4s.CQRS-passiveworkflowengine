package at.jku.isse.passiveprocessengine.frontend.ui.utils;

import java.util.Properties;

public class UIConfig extends Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public boolean isAnonymized() {
		return Boolean.valueOf(this.getProperty("isAnonymized", "false"));
	}
	
	public boolean doGenerateRefetchButtonsPerArtifact() {
		return Boolean.valueOf(this.getProperty("doGenerateRefetchButtonsPerArtifact", "false"));
	}
}
