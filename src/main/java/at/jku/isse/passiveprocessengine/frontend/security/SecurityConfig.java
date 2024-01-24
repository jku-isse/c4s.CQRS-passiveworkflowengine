package at.jku.isse.passiveprocessengine.frontend.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;


@EnableWebSecurity
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private static final String LOGIN_PROCESSING_URL = "/login";
  private static final String LOGIN_FAILURE_URL = "/login?error";
  private static final String LOGIN_URL = "/login";
  private static final String LOGOUT_SUCCESS_URL = "/login";

  /**
   * Require login to access internal pages and configure login form.
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Vaadin handles CSRF internally
    http.csrf().disable()

        // Register our CustomRequestCache, which saves unauthorized access attempts, so the user is redirected after login.
        .requestCache().requestCache(new CustomRequestCache())

        // Restrict access to our application.
        .and().authorizeRequests()

        // Allow all Vaadin internal requests.
        .requestMatchers(SecurityUtils::isFrameworkInternalRequest).permitAll()

        // Allow all requests by logged-in users.
        .anyRequest().authenticated()

        // Configure the login page.
        .and().formLogin()
        .loginPage(LOGIN_URL).permitAll()
        .loginProcessingUrl(LOGIN_PROCESSING_URL)
        .failureUrl(LOGIN_FAILURE_URL)

        // Configure logout
        .and().logout().logoutSuccessUrl(LOGOUT_SUCCESS_URL);
    
    // allow embedding of blockly editor as an iframe locally
    //http.headers().frameOptions().sameOrigin();
    http
    .headers().addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN));
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
      return config.getAuthenticationManager();
  }  
  
  
  @Bean
  @Override
  public UserDetailsService userDetailsService() {
	Random rand = new Random(654654l);	  	  	  
	Set<UserDetails> users = new HashSet<>();
	for (int i = 1; i < 50; i++) {
		String name = "P"+i;		
		String pw = RandomStringUtils.random(6, 97, 122 ,true, false, null, rand);						
		users.add(User.withUsername(name)
            .password("{noop}"+pw)
            .roles("USER")
            .build());		
		System.out.println(String.format("Created user credentials %s : %s", name, pw));
    }
	users.add(User.withUsername("dev")
            .password("{noop}dev")
            .roles("USER")
            .build());	
	users.add(User.withUsername("repaironly")
            .password("{noop}repaironly")
            .roles("USER")
            .build());
	users.add(User.withUsername("norepair")
            .password("{noop}norepair")
            .roles("USER")
            .build());
	
    return new InMemoryUserDetailsManager(users);
  }

  public static Map<String,String> getExperimentUserCredentials() {
	  Random rand = new Random(654654l);
	  Map<String, String> credentials = new HashMap<>();
	  for (int i = 1; i < 50; i++) {
			String name = "P"+i;		
			String pw = RandomStringUtils.random(6, 97, 122 ,true, false, null, rand);						
			credentials.put(name, pw);
	  }
	  return credentials;
  }
  
  /**
   * Allows access to static resources, bypassing Spring Security.
   */
  @Override
  public void configure(WebSecurity web) {
    web.ignoring().antMatchers(
        // webhook endpoints
    	"/azure/**",
    	"/jira/**",
    	"/jama/**",
    	"/ceps/**",	
    	"/transform**",	
    	"/deploySnapshot**",
    	"/deploySnapshotFromXML**",
    	"/deployResult**",
    	"/processlogs/**",
    	"/repairstatistics**",	
    	"/qastatistics**",	
    	"/participants**",
    	"/stages/**",
    	
    	// Client-side JS
        "/VAADIN/**",

        // the standard favicon URI
        "/favicon.ico",

        // the robots exclusion standard
        "/robots.txt",

        // web application manifest
        "/manifest.webmanifest",
        "/sw.js",
        "/offline.html",

        // icons and images
        "/icons/**",
        "/images/**",
        "/styles/**",

        // (development mode) H2 debugging console
        "/h2-console/**");
  }
}