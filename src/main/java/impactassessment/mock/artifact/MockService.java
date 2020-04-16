package impactassessment.mock.artifact;

import org.springframework.context.annotation.Bean;

public class MockService {
    public static Artifact mockArtifact(String id) {
        Artifact a = new Artifact();
        a.setField("id", id);
        a.setField("status", "Resolved");
        a.setField("issuetype", "Hazard");
        a.setField("priority", "high");
        return a;
    }
}
