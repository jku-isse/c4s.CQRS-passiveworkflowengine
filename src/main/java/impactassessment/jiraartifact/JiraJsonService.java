package impactassessment.jiraartifact;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.json.IssueJsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class JiraJsonService implements IJiraArtifactService {

    @Override
    public IJiraArtifact get(String key) {
        Issue issue = null;
        try {
            issue = loadIssue(key);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        JiraArtifact artifact = null;
        if (issue != null) {
            artifact = new JiraArtifact(issue);
        }
        return artifact;
    }

    public static Issue loadIssue(String key) throws JSONException, IOException {
		InputStream is = JiraJsonService.class.getClassLoader().getResourceAsStream("dronology_jira.json");
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
