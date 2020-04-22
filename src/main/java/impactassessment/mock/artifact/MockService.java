package impactassessment.mock.artifact;

import impactassessment.model.workflowmodel.ResourceLink;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

public class MockService {
    public static Artifact mockArtifact(String id) {
        Artifact a = new Artifact();
        a.setField("id", id);
        a.setField("status", "Resolved");
        a.setField("issuetype", "Hazard");
        a.setField("priority", "high");
        a.setField("summary", "This summarizes the artifact!");
        return a;
    }

    public static ResourceLink getHumanReadableResourceLinkEndpoint(Artifact a) {
        Optional<String> summary = Optional.of(a.getField("summary"));
        Optional<String> issueType = Optional.of(a.getField("issuetype"));
        String title = a.getId();
        return new ResourceLink(summary.orElse("failed"), "www.somwhere.com", "self", issueType.orElse("failed") ,"html", title);
    }
}
