package impactassessment.artifactconnector.jira.subinterfaces;

public interface IJiraLink {

    IJiraIssueLink getSource();

    IJiraIssueLink getTarget();

}
