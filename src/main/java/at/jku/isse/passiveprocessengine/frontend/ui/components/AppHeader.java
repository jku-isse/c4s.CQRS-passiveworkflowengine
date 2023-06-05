package at.jku.isse.passiveprocessengine.frontend.ui.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;

public class AppHeader extends HorizontalLayout{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private SecurityService sec;

	public AppHeader(String pageTitle, SecurityService securityService /*, RefreshableComponent comp*/) {
		this.setClassName("header-theme");
        this.setMargin(false);
        this.setPadding(true);
        this.setSizeFull();
        this.setHeight("6%");
        HorizontalLayout firstPart = new HorizontalLayout();
        firstPart.setClassName("header-theme");
        firstPart.setMargin(false);
        firstPart.setPadding(true);
        firstPart.setSizeFull();
        firstPart.add(new Icon(VaadinIcon.CLUSTER), new Label(""), new Text(pageTitle));
        this.sec = securityService;
//        Button toggle = new Button("Refresh");
//        toggle.setClassName("med");
//        toggle.addClickListener(evt -> {
//            comp.refreshContent();
//        });

        Button progress = new Button("Connector Progress");         
        progress.setClassName("med");
        progress.addClickListener(evt -> {
        	UI.getCurrent().navigate("progress");
        });
        
        Button arl = new Button("Constraint Editor");
        arl.setClassName("med");
        arl.addClickListener(evt -> {
        	UI.getCurrent().navigate("arl");
        });
        
        Button home = new Button("Home");
        home.setClassName("med");
        home.addClickListener(evt -> {
        	UI.getCurrent().navigate("home");
        });
        
        Button logout = new Button("Logout");
        logout.setClassName("med");
        logout.addClickListener(evt -> {
        	sec.logout();
        });
        
        
        this.add(firstPart, home, progress, arl, logout/*, toggle*/);
        this.setJustifyContentMode(JustifyContentMode.BETWEEN);
	}
}
