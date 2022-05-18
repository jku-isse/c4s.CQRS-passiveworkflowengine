package at.jku.isse;

import at.jku.isse.designspace.artifactconnector.core.updatememory.UpdateMemory;
import at.jku.isse.designspace.artifactconnector.core.updateservice.UpdateManager;
import at.jku.isse.designspace.core.controlflow.ControlEventEngine;
import at.jku.isse.designspace.core.events.Event;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.artifacts.AzureServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.artifacts.DemoServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.artifacts.GitServiceWrapper;
import at.jku.isse.passiveprocessengine.frontend.registry.AbstractProcessLoader;
import at.jku.isse.passiveprocessengine.frontend.registry.ProcessSpecificationLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

//import javax.jms.ConnectionFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;



@Configuration
@Slf4j
public class FrontendSpringConfig {


	private UpdateMemory updateMemory;


	private UpdateManager updateManager;

	@Bean
	@DependsOn({"controlEventEngine"})
	public UpdateMemory initUpdateMemory() {
		this.updateMemory = new UpdateMemory();
		return this.updateMemory;
	}

	@Bean
	@DependsOn({"controlEventEngine"})
	public UpdateManager initUpdateManager() {
		this.updateManager = new UpdateManager();
		updateManager.init();
		return this.updateManager;
	}

	//------------------------------------------------------------------------------------------------------------------
	//-------------------------------------------PROJECT COMPONENTS-----------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------


	@Bean 
	@DependsOn({"controlEventEngine"})
	ProcessRegistry getProcessRegistry() {
		return new ProcessRegistry();
	}

	@Bean
	@DependsOn({"controlEventEngine"})
	public AbstractProcessLoader getProcessLoader(ProcessRegistry registry) {
		return new ProcessSpecificationLoader(registry);
	}

	@Bean
	@DependsOn({"controlEventEngine"})
	public ArtifactResolver getArtifactResolver(AzureServiceWrapper azure, GitServiceWrapper github, DemoServiceWrapper demo, ProcessRegistry procReg ) {
		ArtifactResolver ar = new ArtifactResolver();
		ar.register(azure);
		ar.register(github);
		ar.register(demo);
		return ar;
	}

    @Bean("controlEventEngine")
    public ControlEventEngine initControlEventEngine(WorkspaceService ws, RuleService rSerice) {
    	Event.setInitialized();
    	Properties props = new Properties();
        try {
            FileReader reader = new FileReader("./application.properties");
            props.load(reader);
            String property = props.getProperty("persistence.enabled");  
            if (property != null) {
                boolean persistenceEnabled = Boolean.parseBoolean(property);
                if (persistenceEnabled) {
                	Path currentRelativePath = Paths.get("");
                    String absPath = currentRelativePath.toAbsolutePath().toString();
                    String path = absPath+ props.getProperty("persistencePath").trim().replace('/', '\\');;
                    ControlEventEngine.initWithPath(path, false);
                }
            }
        } catch (IOException ioe) {
            Workspace.logger.debug("CORE-SERVICE: The running directory did not contain an application.properties file, Persistence cannot be initialized!");
        }
        return null;
    }
	

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}



}
