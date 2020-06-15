package impactassessment.artifact.mock;

import impactassessment.artifact.jira.JiraArtifact;

public class MockService {

    // Field names
    public static final String ID = "id";
    public static final String STATUS = "status";
    public static final String ISSUETYPE = "issuetype";
    public static final String PRIORITY = "priority";
    public static final String SUMMARY = "summary";
    // Default values
    public static final String DEFAULT_STATUS = "Resolved";
    public static final String DEFAULT_ISSUETYPE = "Hazard";
    public static final String DEFAULT_PRIORITY = "high";
    public static final String DEFAULT_SUMMARY = "This summarizes the artifact!";

    public static JiraArtifact mockArtifact(String id) {
        return mockArtifact(id, DEFAULT_STATUS);
    }

    public static JiraArtifact mockArtifact(String id, String status) {
        return mockArtifact(id, status, DEFAULT_ISSUETYPE);
    }

    public static JiraArtifact mockArtifact(String id, String status, String issuetype) {
        return mockArtifact(id, status, issuetype, DEFAULT_PRIORITY);
    }

    public static JiraArtifact mockArtifact(String id, String status, String issuetype, String priority) {
        return mockArtifact(id, status, issuetype, priority, DEFAULT_SUMMARY);
    }

    public static JiraArtifact mockArtifact(String id, String status, String issuetype, String priority, String summary) {
//        JiraArtifact a = new JiraArtifact();
//        a.setField(ID, id);
//        a.setField(STATUS, status);
//        a.setField(ISSUETYPE, issuetype);
//        a.setField(PRIORITY, priority);
//        a.setField(SUMMARY, summary);
//        return a;
        return null;
    }

}
