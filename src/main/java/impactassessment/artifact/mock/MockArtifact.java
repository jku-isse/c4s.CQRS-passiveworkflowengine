package impactassessment.artifact.mock;

import impactassessment.artifact.base.*;
import org.joda.time.DateTime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

public class MockArtifact implements IArtifact {

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
            return new URI("dummy-URI");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public IStatus getStatus() {
        return new IStatus() {
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
            public IStatusCategory getStatusCategory() {
                return null;
            }
        };
    }

    @Override
    public IUser getReporter() {
        return null;
    }

    @Override
    public IUser getAssignee() {
        return null;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public IBasicPriority getPriority() {
        return new IBasicPriority() {
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
    public Iterable<IIssueLink> getIssueLinks() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<ISubtask> getSubtasks() {
        return null;
    }

    @Override
    public Iterable<IIssueField> getFields() {
        return Collections.emptyList();
    }

    @Override
    public IIssueField getField(String id) {
        return null;
    }

    @Override
    public IIssueField getFieldByName(String name) {
        return null;
    }

    @Override
    public IIssueType getIssueType() {
        return new IIssueType() {
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
    public IBasicProject getProject() {
        return null;
    }

    @Override
    public IBasicVotes getVotes() {
        return null;
    }

    @Override
    public Iterable<IVersion> getFixVersions() {
        return null;
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
