package impactassessment.jiraartifact.mock;

import impactassessment.jiraartifact.*;
import impactassessment.jiraartifact.subinterfaces.*;
import org.joda.time.DateTime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

public class JiraMockArtifact implements IJiraArtifact {

    private String id;
    private String status;
    private String issueType;
    private String priority;
    private String summary;

    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public URI getSelf() {
        try {
            return new URI("dummy");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public URI getBrowserLink() {
        try {
            return new URI("dummy");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getKey() {
        return id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public IJiraStatus getStatus() {
        return new IJiraStatus() {
            @Override
            public URI getSelf() {
                return null;
            }

            @Override
            public String getName() {
                return status;
            }

            @Override
            public Long getId() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public IJiraStatusCategory getStatusCategory() {
                return null;
            }
        };
    }

    @Override
    public IJiraUser getReporter() {
        return null;
    }

    @Override
    public IJiraUser getAssignee() {
        return null;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public IJiraBasicPriority getPriority() {
        return new IJiraBasicPriority() {
            @Override
            public URI getSelf() {
                return null;
            }

            @Override
            public String getName() {
                return priority;
            }

            @Override
            public Long getId() {
                return null;
            }
        };
    }

    @Override
    public Iterable<IJiraIssueLink> getIssueLinks() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<IJiraSubtask> getSubtasks() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<IJiraIssueField> getFields() {
        return Collections.emptyList();
    }

    @Override
    public IJiraIssueField getField(String id) {
        return null;
    }

    @Override
    public IJiraIssueField getFieldByName(String name) {
        return null;
    }

    @Override
    public IJiraIssueType getIssueType() {
        return new IJiraIssueType() {
            @Override
            public Long getId() {
                return null;
            }

            @Override
            public String getName() {
                return issueType;
            }

            @Override
            public boolean isSubtask() {
                return false;
            }

            @Override
            public URI getSelf() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }
        };
    }

    @Override
    public IJiraBasicProject getProject() {
        return null;
    }

    @Override
    public IJiraBasicVotes getVotes() {
        return null;
    }

    @Override
    public Iterable<IJiraVersion> getFixVersions() {
        return Collections.emptyList();
    }

    @Override
    public DateTime getCreationDate() {
        return null;
    }

    @Override
    public DateTime getUpdateDate() {
        return null;
    }

    @Override
    public DateTime getDueDate() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
