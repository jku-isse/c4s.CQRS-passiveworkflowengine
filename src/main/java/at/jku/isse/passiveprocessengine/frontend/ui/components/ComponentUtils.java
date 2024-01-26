package at.jku.isse.passiveprocessengine.frontend.ui.components;

import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Anchor;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;

public class ComponentUtils {

	public static Anchor convertToResourceLinkWithBlankTarget(Instance artifact) {
		Anchor a;
		if (artifact.hasProperty("html_url") && artifact.getPropertyAsValue("html_url") != null) {
			a = new Anchor(artifact.getPropertyAsValue("html_url").toString(), generateDisplayNameForInstance(artifact));
		} else {
			a = new Anchor("/instance/"+artifact.id(), generateDisplayNameForInstance(artifact));
		}
		a.setTarget("_blank");
		return a;
	}
	
	public static Anchor convertToResourceLinkWithBlankTarget(InstanceType artifact) {
		Anchor a;
		if (artifact.hasProperty("html_url")) {
			a = new Anchor(artifact.getPropertyAsValue("html_url").toString(), artifact.name());
		} else {
			a = new Anchor("/instance/"+artifact.id(), artifact.name());
		}
		a.setTarget("_blank");
		return a;
	}

	public static String generateDisplayNameForInstance(Instance inst) {
		if (inst.hasProperty("title") && inst.hasProperty("workItemType")) { // FIXME assume we have a azure item
			String title = (String) inst.getPropertyAsValueOrElse("title", () -> "Unknown");
			String id = (String) inst.getPropertyAsValue("id");
			//String type = inst.getPropertyAsInstance("workItemType") != null ? inst.getPropertyAsInstance("workItemType").name() : "UnknownType";
			String type = (String) inst.getPropertyAsValue("workItemType"); 
			return "["+type+"]"+id+":"+title;
		} else if (inst.hasProperty("linkType") && inst.hasProperty("linkTo")) { //azure link type
			String type = inst.getPropertyAsInstance("linkType").name();			
			String title = generateDisplayNameForInstance(inst.getPropertyAsInstance("linkTo"));
			return type+":"+title;
		} if (inst.hasProperty("title") && inst.hasProperty("key") && inst.hasProperty("labels")) { // FIXME assume we have a github issue
			String key = (String) inst.getPropertyAsValueOrElse("key", () -> "Unknown");
			String title = (String) inst.getPropertyAsValueOrElse("title", () -> "Unknown");
			String labels = (String) inst.getPropertyAsList("labels").stream().collect(Collectors.joining(",", "[", "]"));
			return String.format("[%s] %s %s", key, title, labels);
		} else if (inst.hasProperty("typeKey")) { // FIXME we assume we have a jama issue 
			String key = (String) inst.getPropertyAsValueOrElse("typeKey", () -> "Unknown");
			String title = (String) inst.getPropertyAsValueOrElse("name", () -> "Unknown");			
			Object statusObj = (String) inst.getPropertyAsValueOrElse("status", () -> "Unknown");
			String status = statusObj != null ? "("+statusObj.toString()+")" : "";
			return String.format("[%s] %s %s", key, title, status);
		} else if (inst.hasProperty("stepDefinition")) // a step 
			return inst.getPropertyAsInstance("stepDefinition").name();
		else { 
			return inst.name();
		}
	}

}
