package impactassessment.passiveprocessengine;

import artifactapi.jama.IJamaArtifact;
import artifactapi.jira.IJiraArtifact;
import passiveprocessengine.instance.ResourceLink;

public class ResourceLinkFactory {

    public static ResourceLink get(IJamaArtifact a) {
        return new ResourceLink(String.valueOf(a.getId()), /* TODO: link?*/"link-into-jama/"+a.getId(), "self", a.getName(), "html", a.getDocumentKey());
    }

    public static ResourceLink get(IJiraArtifact a) {
        return new ResourceLink(a.getSummary(), a.getBrowserLink().toString(), "self", a.getIssueType().getName(), "html", a.getKey());
    }

    public static ResourceLink getMock() {
        String s = "placeholder";
        return new ResourceLink(s,s,s,s,s,s);
    }
}
