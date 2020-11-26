package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.domain.IssueLinkType;
import impactassessment.jiraartifact.subinterfaces.IJiraIssueLinkType;

public class JiraIssueLinkType implements IJiraIssueLinkType {

    private IssueLinkType issueLinkType;

    public JiraIssueLinkType(IssueLinkType issueLinkType) {
        this.issueLinkType = issueLinkType;
    }

    @Override
    public String getName() {
        return issueLinkType.getName();
    }

    @Override
    public String getDescription() {
        return issueLinkType.getDescription();
    }

    @Override
    public Direction getDirection() {
        if (issueLinkType.getDirection() == IssueLinkType.Direction.OUTBOUND) {
            return Direction.OUTBOUND;
        } else {
            return Direction.INBOUND;
        }
    }
}
