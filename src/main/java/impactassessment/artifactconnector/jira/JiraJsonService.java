package impactassessment.artifactconnector.jira;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.jira.IJiraArtifact;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.json.IssueJsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
public class JiraJsonService implements IArtifactService, IJiraService {

    private final String FOLDER_NAME = "demo";

    JSONArray issues = new JSONArray();
    
    public JiraJsonService() {
    	try {
			FileFilter fileFilter = new WildcardFileFilter("*.json");
			File[] files = new File("./"+FOLDER_NAME+"/").listFiles(fileFilter);
			if (files != null) 
				for (File file : files) {
					InputStream is = new FileInputStream(file);
					String body = IOUtils.toString(is, StandardCharsets.UTF_8);
			    	JSONObject issueAsJson = new JSONObject(body);
			    	issues = issueAsJson.getJSONArray("issues");
			    	break;
				}
		} catch (IOException | JSONException e) {
			log.warn("Unable to load json DEMO data", e);
		}
    }
    
    @Override
    public boolean provides(String s) {
        return s.equals(IJiraArtifact.class.getSimpleName());
    }

    @Override
    public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
    	Optional<IJiraArtifact> optArt = getIssue(id.getId(), workflowId);
    	if (optArt.isPresent())
    		return Optional.of( (IArtifact)optArt.get() );
    	return Optional.empty();
    }

    /**
     * will search in every json file inside the demo folder
     */
    private Optional<Issue> loadIssue(String key) throws JSONException, IOException {

    	JSONObject jsonObj = null;
    	for (int i = 0; i < issues.length(); i++) {
    		JSONObject curIssue = issues.getJSONObject(i);
    		if (curIssue.getString("key").equals(key)) {
    			jsonObj = curIssue;
    			break;
    		}
    	}
    	if (jsonObj != null) {
    		return Optional.of(new IssueJsonParser(new JSONObject(), new JSONObject()).parse(jsonObj));
    	}

    	log.warn("Issue {} wasn't found inside any json file", key);
    	return Optional.empty();
	}

    @Override
    public void injectArtifactService(IArtifact iArtifact, String s) {
//        log.warn("injectArtifactService not implemented in JiraJsonService!");
    	iArtifact.injectArtifactService(this);
    }

    @Override
    public void deleteDataScope(String s) {
        log.info("deleteDataScope not implemented in JiraJsonService!");
    }

    @Override
    public Optional<IJiraArtifact> getIssue(String id, String workflow) {
        log.debug("JiraJsonService loads "+id);
        try {
            Optional<Issue> opt = loadIssue(id);
            if (opt.isPresent()) {
                IJiraArtifact artifact = new JiraArtifact(opt.get(), this);
                return Optional.of(artifact);
            }
        } catch (JSONException | IOException e) {
            log.error(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<IJiraArtifact> getIssue(String key) {
        return getIssue(key, null);
    }
}
