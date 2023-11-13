package at.jku.isse.passiveprocessengine.frontend.ui.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;

public class AppFooter extends HorizontalLayout{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AppFooter(UIConfig conf) {
		this.setClassName("footer-theme");
	    if (conf.isAnonymized())        
	    	this.add(new Text("Version "+conf.getVersion()+" (C) 2023 - Anonymized "));
	    else 
	    	this.add(new Text("Version "+conf.getVersion()+" (C) 2023 - JKU - Institute for Software Systems Engineering"));
	}
	
	
    
}
