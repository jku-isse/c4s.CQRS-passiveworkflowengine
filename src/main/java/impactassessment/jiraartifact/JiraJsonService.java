package impactassessment.jiraartifact;

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
import java.util.Properties;

@Slf4j
public class JiraJsonService implements IJiraArtifactService {

    private final String FILENAME;

    private JiraChangeSubscriber jiraChangeSubscriber;

    public JiraJsonService(JiraChangeSubscriber jiraChangeSubscriber) {
        this.jiraChangeSubscriber = jiraChangeSubscriber;

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
    public IJiraArtifact get(String artifactKey, String workflowId) {
        log.debug("JiraJsonService loads "+artifactKey);
        Issue issue = null;
        try {
            issue = loadIssue(artifactKey);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        if (issue == null)
            return null;

        jiraChangeSubscriber.addUsage(workflowId, artifactKey);

        return new JiraArtifact(issue);
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

}
