package at.jku.isse.passiveprocessengine.frontend.ui.utils;

import java.util.Properties;

public class UIConfig extends Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private final String version;
	
	public UIConfig(String version) {
		this.version = version;
	}

	public boolean isAnonymized() {
		return Boolean.valueOf(this.getProperty("isAnonymized", "false"));
	}
	
	public boolean doGenerateRefetchButtonsPerArtifact() {
		return Boolean.valueOf(this.getProperty("doGenerateRefetchButtonsPerArtifact", "false"));
	}
	
	public boolean doUseIntegratedEvalRepairTree() {
		return Boolean.valueOf(this.getProperty("doUseIntegratedEvalRepairTree", "false"));
	}
	
	public boolean doEnableExperimentMode() {
		return Boolean.valueOf(this.getProperty("enableExperimentMode", "false"));
	}
	
    public String getVersion() {
        return version;
    }
}
