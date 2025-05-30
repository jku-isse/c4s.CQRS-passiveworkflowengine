package at.jku.isse.passiveprocessengine.frontend.ui;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;

@Route("login")
@PageTitle("Login Passive Process Engine Dashboard")
public class LoginView extends VerticalLayout implements BeforeEnterObserver{

	private final LoginForm login = new LoginForm();
	
	public LoginView(){
		addClassName("login-view");
		setSizeFull(); 

		setAlignItems(Alignment.CENTER);
		setJustifyContentMode(JustifyContentMode.CENTER);

		login.setAction("login"); 

		Anchor googleLoginLink = new Anchor(ComponentUtils.getBaseUrl()+"/oauth2/authorization/google", "Login with Google");

		add(new H1("Passive Process Engine Dashboard"), login, new Html("<hr>"), googleLoginLink);
	}
	
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if(event.getLocation()  
		        .getQueryParameters()
		        .getParameters()
		        .containsKey("error")) {
		            login.setError(true);
		        }
	}

}
