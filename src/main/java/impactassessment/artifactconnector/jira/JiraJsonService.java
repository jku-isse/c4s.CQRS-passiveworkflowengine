package impactassessment.artifactconnector.jira;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.jira.IJiraArtifact;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.json.IssueJsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class JiraJsonService implements IArtifactService, IJiraService {

    private final String FILENAME;
    private static final String TYPE = IJiraArtifact.class.getSimpleName();

    private JiraDataScope jds;

    private JiraChangeSubscriber jiraChangeSubscriber;

    public JiraJsonService(JiraChangeSubscriber jiraChangeSubscriber) {
        this.jiraChangeSubscriber = jiraChangeSubscriber;
        this.jds = new JiraDataScope("mock", this);
        Properties props = new Properties();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("application.properties").getFile());
            FileReader reader = new FileReader(file);
            props.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.FILENAME = props.getProperty("jiraJsonFileName");
    }

    @Override
    public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
        log.debug("JiraJsonService loads "+id.getId());
        Issue issue = null;
        try {
            issue = loadIssue(id.getId());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        if (issue == null)
            return null;

        jiraChangeSubscriber.addUsage(jds, id);
        IArtifact artifact = new JiraArtifact(issue, jds);
        return Optional.of(artifact);
    }

    @Override
    public void injectArtifactService(IArtifact iArtifact, String s) {
        // TODO not implemented
    }

    @Override
    public void deleteDataScope(String s) {

    }

    private Issue loadIssue(String key) throws JSONException, IOException {
		InputStream is = JiraJsonService.class.getClassLoader().getResourceAsStream(FILENAME);
        String body = IOUtils.toString(is, "UTF-8");
        JSONObject issueAsJson = new JSONObject(body);
        JSONArray issues = issueAsJson.getJSONArray("issues");
        JSONObject jsonObj = null;
        for (int i = 0; i < issues.length(); i++) {
            JSONObject curIssue = issues.getJSONObject(i);
            if (curIssue.getString("key").equals(key)) {
                jsonObj = curIssue;
                break;
            }
        }
        Issue issue = null;
        if (jsonObj != null) {
            issue = new IssueJsonParser(new JSONObject(), new JSONObject()).parse(jsonObj);
        }
        // log.info("Parsed issue:\n" + issue);
        return issue;
	}

    @Override
    public boolean provides(String type) {
        return type.equals(TYPE);
    }

	@Override
	public Optional<IJiraArtifact> getIssue(String id, String workflow) {
		Optional<IArtifact> opt = get(new ArtifactIdentifier(id, ""), workflow);
		if (opt.isPresent()) 
			return Optional.ofNullable((IJiraArtifact)opt.get());
		else 
			return Optional.empty();
	}

	@Override
	public Optional<IJiraArtifact> getIssue(String key) {
		return getIssue(key, "mock");
	}


}
