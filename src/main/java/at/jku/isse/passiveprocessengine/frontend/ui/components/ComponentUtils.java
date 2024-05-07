package at.jku.isse.passiveprocessengine.frontend.ui.components;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Anchor;

import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;


public class ComponentUtils {

	public static Anchor convertToResourceLinkWithBlankTarget(PPEInstance artifact) {
		Anchor a;
		if (artifact.getInstanceType().hasPropertyType("html_url") && artifact.getTypedProperty("html_url", String.class) != null) {
			a = new Anchor(artifact.getTypedProperty("html_url", String.class), generateDisplayNameForInstance(artifact));
		} else {
			a = new Anchor("/instance/"+artifact.getId(), generateDisplayNameForInstance(artifact));
		}
		a.setTarget("_blank");
		return a;
	}
	
	public static Anchor convertToResourceLinkWithBlankTarget(PPEInstanceType artifact) {
		Anchor a;
		if (artifact.hasPropertyType("html_url")) {
			a = new Anchor(artifact.getTypedProperty("html_url", String.class), artifact.getName());
		} else {
			a = new Anchor("/instancetype/"+artifact.getId(), artifact.getName());
		}
		a.setTarget("_blank");
		return a;
	}

	public static String generateDisplayNameForInstance(PPEInstance inst) {
		if (inst.getInstanceType().hasPropertyType("title") && inst.getInstanceType().hasPropertyType("workItemType")) { // FIXME assume we have a azure item
			String title = inst.getTypedProperty("title", String.class, "Unknown");
			String id = inst.getTypedProperty(CoreTypeFactory.EXTERNAL_DEFAULT_ID, String.class);
			//String type = inst.getPropertyAsInstance("workItemType") != null ? inst.getPropertyAsInstance("workItemType").name() : "UnknownType";
			String type = (String) inst.getTypedProperty("workItemType", String.class); 
			return "["+type+"]"+id+":"+title;
		} if (inst.getInstanceType().hasPropertyType("title") && inst.getInstanceType().hasPropertyType("key") && inst.getInstanceType().hasPropertyType("labels")) { // FIXME assume we have a github issue
			String key = (String) inst.getTypedProperty("key",  String.class, "Unknown");
			String title = (String) inst.getTypedProperty("title",  String.class, "Unknown");
			String labels = (String) inst.getTypedProperty("labels", List.class).stream().collect(Collectors.joining(",", "[", "]"));
			return String.format("[%s] %s %s", key, title, labels);
		} else if (inst.getInstanceType().hasPropertyType("typeKey")) { // FIXME we assume we have a jama issue 
			String key = (String) inst.getTypedProperty("typeKey", String.class, "Unknown");
			String title = (String) inst.getTypedProperty("name", String.class, "Unknown");			
			Object statusObj = (String) inst.getTypedProperty("status", String.class, "Unknown");
			String status = statusObj != null ? "("+statusObj.toString()+")" : "";
			return String.format("[%s] %s %s", key, title, status);
		} else if (inst.getInstanceType().hasPropertyType("stepDefinition")) // a step 
			return inst.getTypedProperty("stepDefinition", PPEInstance.class).getName();
		else { 
			return inst.getName();
		}
	}

}
