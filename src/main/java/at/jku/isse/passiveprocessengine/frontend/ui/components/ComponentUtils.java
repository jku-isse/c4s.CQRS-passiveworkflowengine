package at.jku.isse.passiveprocessengine.frontend.ui.components;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.VaadinServlet;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.CoreTypeFactory;


public class ComponentUtils {

	public static String getBaseUrl() {
		return VaadinServlet.getCurrent().getServletContext().getContextPath();		
	}
	
	public static String getRelativeBaseUrl() {
		String fullBaseUrl = VaadinServlet.getCurrent().getServletContext().getContextPath();
		URI withoutPath = URI.create(fullBaseUrl).resolve("/");
		String prefix = withoutPath.toString();
		if (prefix.length() > fullBaseUrl.length())
			return fullBaseUrl;
		else
			return fullBaseUrl.substring(prefix.length());		
	}
	
	public static Anchor convertToResourceLinkWithBlankTarget(PPEInstance artifact) {
		Anchor a;
		if (artifact.getInstanceType().hasPropertyType(CoreTypeFactory.URL_URI) && artifact.getTypedProperty(CoreTypeFactory.URL_URI, String.class) != null) {
			a = new Anchor(artifact.getTypedProperty(CoreTypeFactory.URL_URI, String.class), generateDisplayNameForInstance(artifact));
		} else {
			a = new Anchor(getBaseUrl()+"/instance/"+artifact.getId(), generateDisplayNameForInstance(artifact));
		}
		a.setTarget("_blank");
		return a;
	}
	
	public static Anchor convertToResourceLinkWithBlankTarget(PPEInstanceType artifact) {
		Anchor a;
		if (artifact.getInstanceType() != null && artifact.getInstanceType().hasPropertyType(CoreTypeFactory.URL_URI)) { // we need to check not whether this instancetype defines a propertyType of URL but whether it has one itself, via checking its parent
			a = new Anchor(artifact.getTypedProperty(CoreTypeFactory.URL_URI, String.class, getBaseUrl()+"/instancetype/"+artifact.getId()), artifact.getName());
		} else {
			a = new Anchor(getBaseUrl()+"/instancetype/"+artifact.getId(), artifact.getName());
		}
		a.setTarget("_blank");
		return a;
	}

	public static String generateDisplayNameForInstance(PPEInstance inst) {
		if (inst.getInstanceType().hasPropertyType("title") && inst.getInstanceType().hasPropertyType("workItemType")) { // FIXME assume we have a azure item
			String title = inst.getTypedProperty("title", String.class, "Unknown");
			String id = inst.getTypedProperty(CoreTypeFactory.EXTERNAL_DEFAULT_ID_URI, String.class);
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
