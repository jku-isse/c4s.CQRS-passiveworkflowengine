package at.jku.isse.passiveprocessengine.frontend.experiment;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
//@ConditionalOnExpression(value = "${enableExperimentMode:false}")
public class ExperimentSequenceProvider {

	public static final String FILENAME = "./experiment/taskorder.json";
	private Map<String,ExperimentSequence> data = Collections.emptyMap();
	
	public ExperimentSequenceProvider() {
		try {
			init();
		} catch (FileNotFoundException e) {						
			log.info("No experiment task order file found at: "+FILENAME);
		} catch (JsonIOException e1) {
			log.warn("Could not read task order file: "+e1.getMessage());			
		} catch (JsonSyntaxException e2) {
			log.warn("Could not read task order file: "+e2.getMessage());
		}
	}
	
	private void init() throws JsonIOException, JsonSyntaxException, FileNotFoundException {		
		Gson gson = new GsonBuilder()				 
				 .setPrettyPrinting()
				 .create();
		Type mapType = new TypeToken<Map<String,ExperimentSequence>>() {}.getType();		
		data = gson.fromJson(new FileReader(FILENAME) , mapType);
	}
	
	public ExperimentSequence getSequenceForParticipant(String pId) {
		return data.get(pId);
	}
	
	
}
