package at.jku.isse.passiveprocessengine.frontend.ui.utils;

import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class UIConfig extends Properties {

	public static enum Keys { 
		anonymization_enabled("anonymization.enabled"),
		generateRefetchButtonsPerArtifact_enabled("generateRefetchButtonsPerArtifact.enabled"),
		integratedEvalRepairTree_enabled("integratedEvalRepairTree.enabled"),
		experimentMode_enabled("experimentMode.enabled"),
		openai_enabled("openai.enabled"),
		blocklyeditor_enabled("blocklyeditor.enabled"),
		stages_enabled("stages.enabled");
		
		private final String label;
		private Keys(String label) { this.label = label; }
		@Override
		public String toString() {
			return this.label;
		}
	}; 
	
	public static Set<String> getWellknownConfigProperties() {
		return Arrays.asList(Keys.values()).stream().map(key -> key.toString()).collect(Collectors.toSet());
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private final String version;
	
	public UIConfig(String version) {
		this.version = version;
	}

	public boolean isAnonymized() {
		return Boolean.valueOf(this.getProperty(Keys.anonymization_enabled.toString(), "false"));
	}
	
	public boolean isGenerateRefetchButtonsPerArtifactEnabled() {
		return Boolean.valueOf(this.getProperty(Keys.generateRefetchButtonsPerArtifact_enabled.toString(), "false"));
	}
	
	public boolean isIntegratedEvalRepairTreeEnabled() {
		return Boolean.valueOf(this.getProperty(Keys.integratedEvalRepairTree_enabled.toString(), "false"));
	}
	
	public boolean isExperimentModeEnabled() {
		return Boolean.valueOf(this.getProperty(Keys.experimentMode_enabled.toString(), "false"));
	}	    

	public boolean isARLBotSupportEnabled() {
		return Boolean.valueOf(this.getProperty(Keys.openai_enabled.toString(), "false"));
	}
	
	public boolean isStagesEnabled() {
		return Boolean.valueOf(this.getProperty(Keys.stages_enabled.toString(), "false"));
	}
	
	public boolean isBlocklyEditorEnabled() {
		return Boolean.valueOf(this.getProperty(Keys.blocklyeditor_enabled.toString(), "true"));
	}
	
	public String getVersion() {
        return version;
    }
}
