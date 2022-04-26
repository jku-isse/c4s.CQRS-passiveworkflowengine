package at.jku.isse.passiveprocessengine.frontend.ui;

import com.vaadin.flow.component.html.Anchor;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;

public class ComponentUtils {

	public static Anchor convertToResourceLinkWithBlankTarget(Instance artifact) {
		Anchor a;
		if (artifact.hasProperty("html_url") && artifact.getPropertyAsValue("html_url") != null) {
			a = new Anchor(artifact.getPropertyAsValue("html_url").toString(), artifact.name());
		} else {
			a = new Anchor("/instance/show?id="+artifact.id(), artifact.name());
		}
		a.setTarget("_blank");
		return a;
	}
	
	public static Anchor convertToResourceLinkWithBlankTarget(InstanceType artifact) {
		Anchor a;
		if (artifact.hasProperty("html_url")) {
			a = new Anchor(artifact.getPropertyAsValue("html_url").toString(), artifact.name());
		} else {
			a = new Anchor("/instance/show?id="+artifact.id(), artifact.name());
		}
		a.setTarget("_blank");
		return a;
	}

}
