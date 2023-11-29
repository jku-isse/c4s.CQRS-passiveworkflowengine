package at.jku.isse.passiveprocessengine.rest;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry.ProcessDeployResult;
import at.jku.isse.passiveprocessengine.frontend.registry.DeployResultPersistence;
import c4s.processdefinition.blockly2java.Transformer;
import c4s.processdefinition.blockly2java.Xml2Java;
import https.developers_google_com.blockly.xml.Xml;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")//to allow incoming calls from other ports
public class ProcessDeployEndpoint {

	private static final JsonDefinitionSerializer serializer = new JsonDefinitionSerializer();

	private static final Gson gson = new GsonBuilder()
			 .registerTypeAdapterFactory(new at.jku.isse.passiveprocessengine.definition.serialization.TransformationResultAdapterFactory())
			 .setPrettyPrinting()
			 .create();
	
	@Autowired
	ProcessRegistry procReg;
	
	@Autowired 
	DeployResultPersistence results;
	
	//private static Map<String, ProcessDeployResult> results = new HashMap<>();
	
	@GetMapping(value="deployResult/{id}", produces="application/json")
	public ResponseEntity<String> getLatestDeployResult(@PathVariable("id") String id) {
		ProcessDeployResult result = results.getResult(id);
		if (results!=null) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(gson.toJson(result));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{}");
		}
	}

	@PostMapping( value="deploySnapshot", consumes="application/json")
	public ResponseEntity<String> deployProcessJson(@RequestBody String json) { 
		try {
			DTOs.Process procD = serializer.fromJson(json);
			if (procD != null) {
				ProcessDeployResult result = procReg.createOrReplaceProcessDefinition(procD, true);			
				results.setLastResult(result);				
				if (result.getDefinitionErrors().isEmpty())
					return ResponseEntity.status(HttpStatus.OK)
						.body(gson.toJson(result));
				else
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(gson.toJson(result));
								//.body(pd.getValue().toString());
			}
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Unable to process json");
		} catch (JsonSyntaxException e) {
			log.warn(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Unable to process json");
		}

	}
	
	@PostMapping( value="transform", consumes="application/xml", produces = "application/json")
	public @ResponseBody String processBlocklyXML(@RequestBody String xml) throws JAXBException { //Map<String, Object>
		Xml2Java x2j = new Xml2Java();
		Transformer t = new Transformer();
		Optional<Xml> optRoot = x2j.parse(xml);
		List<String> resp = new LinkedList<>();
		//Map<String, Object> resp = new HashMap<>();
		if (optRoot.isPresent()) {
			t.toProcessDefinition(optRoot.get()).stream().forEach(proc -> {
				JsonDefinitionSerializer dser = new JsonDefinitionSerializer();
				String defString = dser.toJson(proc);
				//resp.put(proc.getCode(), defString);
				resp.add(defString);
			});
		}
		return resp.size() > 0 ? resp.get(0) : "";
		
	}
	
	@PostMapping( value="deploySnapshotFromXML", consumes="application/xml")
	public ResponseEntity<String> deployProcessXML(@RequestBody String xml) { 
		try {
			String json = processBlocklyXML(xml);
			if (json == null)
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Found no processdefinition in XML");
			return deployProcessJson(json);
		} catch (JAXBException e) {
			log.warn(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Unable to process xml");
		}

	}
}
