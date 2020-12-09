package impactassessment.artifactconnector.jira.subtypes;

import com.atlassian.jira.rest.client.api.domain.BasicProject;
import impactassessment.artifactconnector.jira.subinterfaces.IJiraBasicProject;

import javax.annotation.Nullable;
import java.net.URI;

public class JiraBasicProject implements IJiraBasicProject {

    private BasicProject basicProject;

    public JiraBasicProject(BasicProject basicProject) {
        this.basicProject = basicProject;
    }

    @Override
    public URI getSelf() {
        return basicProject.getSelf();
    }

    @Override
    public String getKey() {
        return basicProject.getKey();
    }

    @Nullable
    @Override
    public String getName() {
        return basicProject.getName();
    }

    @Nullable
    @Override
    public Long getId() {
        return basicProject.getId();
    }
}
