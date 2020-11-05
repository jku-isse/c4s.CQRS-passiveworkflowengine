package impactassessment.jiraartifact;

import c4s.jiralightconnector.ChangeSubscriber;
import c4s.jiralightconnector.IssueAgent;
import impactassessment.api.UpdateWorkflowsCmd;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraChangeSubscriber implements ChangeSubscriber {

    private final CommandGateway commandGateway;

    @Override
    public void handleUpdatedIssues(List<IssueAgent> list) {
        log.info("handleUpdateIssues");
        for (IssueAgent issueAgent : list) {
            IJiraArtifact jiraArtifact = new JiraArtifact(issueAgent.getIssue());
            commandGateway.send(new UpdateWorkflowsCmd("update", jiraArtifact));
        }
    }
}
