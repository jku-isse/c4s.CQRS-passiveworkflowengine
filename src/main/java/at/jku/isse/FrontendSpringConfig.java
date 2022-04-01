package at.jku.isse;

import at.jku.isse.designspace.artifactconnector.core.updatememory.UpdateMemory;
import at.jku.isse.designspace.artifactconnector.core.updateservice.UpdateManager;
import at.jku.isse.designspace.core.model.Tool;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.git.connector.GitService;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.artifacts.DemoServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.artifacts.GitServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.registry.AbstractProcessLoader;
import at.jku.isse.passiveprocessengine.frontend.registry.ProcessSpecificationLoader;
import lombok.extern.slf4j.Slf4j;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;



@Configuration
@Slf4j
public class FrontendSpringConfig {


	private UpdateMemory updateMemory;


	private UpdateManager updateManager;

	@Bean
	public UpdateMemory initUpdateMemory() {
		this.updateMemory = new UpdateMemory();
		return this.updateMemory;
	}

	@Bean
	public UpdateManager initUpdateManager() {
		this.updateManager = new UpdateManager();
		updateManager.init();
		return this.updateManager;
	}

	//------------------------------------------------------------------------------------------------------------------
	//-------------------------------------------PROJECT COMPONENTS-----------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------


	@Bean ProcessRegistry getProcessRegistry() {
		return new ProcessRegistry();
	}

	@Bean
	public AbstractProcessLoader getProcessLoader(ProcessRegistry registry) {
		return new ProcessSpecificationLoader(registry);
	}

	@Bean
	public ArtifactResolver getArtifactResolver(GitServiceWrapper github, DemoServiceWrapper demo, ProcessRegistry procReg ) {
		ArtifactResolver ar = new ArtifactResolver();
		ar.register(github);
		ar.register(demo);
		return ar;
	}

//	@Bean
//	@ConditionalOnExpression(value = "${git.enabled:false}")
//	public GitService initGitService(UpdateManager updateManager, UpdateMemory updateMemory) {
//		updateManager.init();
//		return new GitService(updateManager, updateMemory);
//	}

//	@Bean
//	public RequestDelegate getRequestDelegate() {
//		return new RequestDelegate();
//	}
	
	// from: https://spring.io/guides/gs/messaging-jms/


	@Bean
	public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory,
			DefaultJmsListenerContainerFactoryConfigurer configurer) {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		// This provides all boot's default to this factory, including the message converter
		factory.setMessageConverter(simpleJmsMessageConverter());
		configurer.configure(factory, connectionFactory);
		// You could still override some of Boot's default if necessary.
		return factory;
	}

	@Bean // Serialize message content to json using TextMessage
	public MessageConverter simpleJmsMessageConverter() {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		return converter;
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}



}
