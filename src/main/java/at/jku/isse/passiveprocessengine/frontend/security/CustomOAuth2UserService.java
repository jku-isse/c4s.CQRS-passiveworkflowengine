package at.jku.isse.passiveprocessengine.frontend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import at.jku.isse.designspace.jira.service.JiraService;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class CustomOAuth2UserService extends DefaultOAuth2UserService {
	@Autowired
	JiraService jiraService;
	// Flag that allows signing users that are not in the organization
	static boolean allowSignUp = false;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);

		// Check the user's email and deny authentication if it is not in our
		// organization in JIRA
		String email = oAuth2User.getAttribute("email");
		if (!allowSignUp && jiraService.getJiraTicketService().getUserIdByEmail(email) == null) {
			throw new OAuth2AuthenticationException(new OAuth2Error("ACCESS_DENIED"), "Invalid email");
		}

		return oAuth2User;
	}
}