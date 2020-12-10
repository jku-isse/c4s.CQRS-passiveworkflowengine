package impactassessment.passiveprocessengine;

import artifactapi.jira.IJiraArtifact;
import passiveprocessengine.instance.ResourceLink;

public class ResourceLinkFactory {

    // TODO another Method for jama will be needed
    public static ResourceLink get(IJiraArtifact a) {
        return new ResourceLink(a.getSummary(), a.getBrowserLink().toString(), "self", a.getIssueType().getName(), "html", a.getKey());
    }

    public static ResourceLink getMock() {
        String s = "placeholder";
        return new ResourceLink(s,s,s,s,s,s);
    }
}
