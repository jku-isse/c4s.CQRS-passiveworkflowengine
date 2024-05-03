package at.jku.isse.passiveprocessengine.frontend.registry;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.FilesystemProcessDefinitionLoader;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry.ProcessDeployResult;
import c4s.processdefinition.blockly2java.Transformer;
import c4s.processdefinition.blockly2java.Xml2Java;
import https.developers_google_com.blockly.xml.Xml;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TriggeredProcessLoader extends FilesystemProcessDefinitionLoader{

	private static AtomicBoolean isInit = new AtomicBoolean(false);
	
	public TriggeredProcessLoader(ProcessRegistry registry) {
		super(registry);
	}

	/**
	 * Source: https://www.baeldung.com/running-setup-logic-on-startup-in-spring
	 *
	 * "This approach can be used for running logic after the Spring context has been initialized,
	 * so we are not focusing on any particular bean, but waiting for all of them to initialize."
	 *
	 * @param event "In this example we chose the ContextRefreshedEvent.
	 *              Make sure to pick an appropriate event that suits your needs."
	 */
	@EventListener
	public void onApplicationEvent(ApplicationReadyEvent event) { //ContextRefreshedEvent event) {
		boolean shouldInit = isInit.compareAndSet(false, true); // if the current value is (expected to be) false, then set it to true and init once
		if (shouldInit) {
			registerAll();
			registerFromBlocklyXML();
		}
	}


	protected void registerFromBlocklyXML() {
		int i = 0;
		// external resources (same directory as JAR)
		try {
			File directory = new File("./processdefinitions/"); // no longer files in separate directories, all in a single one now
			FileFilter fileFilter = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.toString().endsWith(".xml");
				}}; //new WildcardFileFilter("*.json");
				File[] xmlFiles = directory.listFiles(fileFilter);
				for (File xmlFile : xmlFiles) {                
					byte[] encoded = Files.readAllBytes(xmlFile.toPath());
					String xml = new String(encoded, Charset.defaultCharset());
					String json = processBlocklyXML(xml);
					DTOs.Process procD = serializer.fromJson(json);
					if (procD != null) {
						try {
							if (!registry.getProcessDefinition(procD.getCode(), true).isPresent()) {
								// we wont override correct existing json based definition
								ProcessDeployResult result = registry.createProcessDefinitionIfNotExisting(procD);
								if (result != null && !result.getDefinitionErrors().isEmpty()) {
									log.warn("Error loading process definition from file system: "+procD.getCode()+"\r\n"+result.getDefinitionErrors().toString());
								} else if (result == null) {
									log.info("Loading of process definition "+procD.getCode()+" defered to when workspace is available");
								}
								i++;
							}
						} catch (NullPointerException e) {
							e.printStackTrace();
							log.error(e.getMessage());
						}

					}
				}
		} catch (Exception e) {
			log.info("No external Blockly XML-based process definitions found in ./processdefinitions/ ");
		}
		log.info("registered additional {} XML-based process definitions from folder ./processdefinitions/", i);     
	}

	public String processBlocklyXML(String xml) throws JAXBException { //Map<String, Object>
		Xml2Java x2j = new Xml2Java();
		Transformer t = new Transformer();
		Optional<Xml> optRoot = x2j.parse(xml);
		List<String> resp = new LinkedList<>();
		if (optRoot.isPresent()) {
			t.toProcessDefinition(optRoot.get()).stream().forEach(proc -> {
				JsonDefinitionSerializer dser = new JsonDefinitionSerializer();
				String defString = dser.toJson(proc);
				resp.add(defString);
			});
		}
		return resp.size() > 0 ? resp.get(0) : "";		
	}

}
