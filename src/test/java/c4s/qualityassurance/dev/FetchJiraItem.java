package c4s.qualityassurance.dev;

import artifactapi.jira.IJiraArtifact;
import impactassessment.DevelopmentConfig;
import impactassessment.artifactconnector.jira.IJiraService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.inject.Injector;



public class FetchJiraItem {

	public static void main(String[] args) {		
		IJiraService jiraS = UserStudyJiraConfig.getJiraService(false);	
		String key = "SIELA-8"; 
		IJiraArtifact item = jiraS.getIssue(key).get();
		System.out.println(toString(item));
		//Map obj = (Map)item.getField("customfield_11704").getValue();
		//System.out.println(obj.get("id"));
		//System.out.println(obj.getClass().getCanonicalName());
		//System.exit(0);		
	}
	
	
	public static String toString(IJiraArtifact j) {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("%s : %s \r\n","ID", j.getId()));
		sb.append(String.format("%s : %s \r\n","KEY", j.getKey()));
		sb.append(String.format("%s : %s < %s > \r\n","TYPE", j.getIssueType().getName(), j.getIssueType().getId()));
		sb.append(String.format("%s : %s < %s > \r\n","STATUS", j.getStatus().getName(), j.getStatus().getId()));
		sb.append(String.format("%s : %s \r\n","REPORTER", j.getReporter().getName()));
		sb.append(String.format("%s : %s \r\n","ASSIGNEE", j.getAssignee() != null ? j.getAssignee().getName() : "null"));
		sb.append("Fields: \r\n");
		j.getFields().forEach(field -> {
			sb.append(String.format("%s '%s' : %s \r\n",field.getId(), field.getName(), field.getValue()));	
		});
		sb.append("Links: \r\n");
		j.getIssueLinks().forEach(link -> {
			sb.append(String.format("%s %s : %s \r\n", link.getIssueLinkType().getName(), link.getIssueLinkType().getDirection(), link.getTargetIssueKey()));
		});
		sb.append("Fix Versions: \r\n");
		j.getFixVersions().forEach(fixv -> {
			sb.append(String.format("%s %s : %s \r\n", fixv.getId(), fixv.getName(), fixv.getReleaseDate() != null ? fixv.getReleaseDate() : "null"));
		});
		sb.append("Subtasks: \r\n");
		j.getSubtasks().forEach(subt -> {
			sb.append(String.format("%s %s (%s) : %s  < %s > \r\n",subt.getIssueKey(), subt.getSummary(), subt.getIssueType().getName(), subt.getStatus().getName(), subt.getStatus().getId()));
		});
		
		return sb.toString();
	}
}
