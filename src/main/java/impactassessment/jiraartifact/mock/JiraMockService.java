package impactassessment.jiraartifact.mock;

import impactassessment.jiraartifact.IJiraArtifact;

public class JiraMockService {

    // Default values
    public static final String DEFAULT_STATUS = "Resolved";
    public static final String DEFAULT_ISSUETYPE = "Task";
    public static final String DEFAULT_PRIORITY = "high";
    public static final String DEFAULT_SUMMARY = "This summarizes the artifact!";

    public static IJiraArtifact mockArtifact(String id) {
        return mockArtifact(id, DEFAULT_STATUS);
    }

    public static IJiraArtifact mockArtifact(String id, String status) {
        return mockArtifact(id, status, DEFAULT_ISSUETYPE);
    }

    public static IJiraArtifact mockArtifact(String id, String status, String issuetype) {
        return mockArtifact(id, status, issuetype, DEFAULT_PRIORITY);
    }

    public static IJiraArtifact mockArtifact(String id, String status, String issuetype, String priority) {
        return mockArtifact(id, status, issuetype, priority, DEFAULT_SUMMARY);
    }

    public static IJiraArtifact mockArtifact(String id, String status, String issuetype, String priority, String summary) {
        JiraMockArtifact a = new JiraMockArtifact();
        a.setId(id);
        a.setStatus(status);
        a.setIssueType(issuetype);
        a.setPriority(priority);
        a.setSummary(summary);
        return a;
    }

}
