package impactassessment.artifact.jira;

import com.atlassian.jira.rest.client.api.StatusCategory;
import com.atlassian.jira.rest.client.api.domain.*;
import impactassessment.artifact.base.*;
import impactassessment.model.workflowmodel.ResourceLink;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class JiraArtifact implements IArtifact {

    private Issue issue;

    public JiraArtifact(Issue issue) {
        this.issue = issue;
    }

    public Issue getIssue() {
        return issue;
    }

    @Override
    public URI getSelf() {
        return issue.getSelf();
    }

    @Override
    public URI getBrowserLink() {
        try {
            return new URI("http://"+getSelf().getHost()+":"+getSelf().getPort()+"/browse/"+getKey());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public String getKey() {
        return issue.getKey();
    }

    @Override
    public String getId() {
        return issue.getId().toString();
    }

    @Override
    public IStatus getStatus() {
        return getiStatus(issue.getStatus());
    }

    @NotNull
    private IStatus getiStatus(Status status) {
        return new IStatus() {
            @Override
            public URI getSelf() {
                return status.getSelf();
            }

            @Override
            public String getName() {
                return status.getName();
            }

            @Override
            public Long getId() {
                return status.getId();
            }

            @Override
            public String getDescription() {
                return status.getDescription();
            }

            @Override
            public IStatusCategory getStatusCategory() {
                StatusCategory statusCategory = status.getStatusCategory();
                return new IStatusCategory() {
                    @Override
                    public Long getId() {
                        return statusCategory.getId();
                    }

                    @Override
                    public URI getSelf() {
                        return statusCategory.getSelf();
                    }

                    @Override
                    public String getKey() {
                        return statusCategory.getKey();
                    }

                    @Override
                    public String getName() {
                        return statusCategory.getName();
                    }
                };
            }
        };
    }

    @Override
    public IUser getReporter() {
        User user = issue.getReporter();
        return getiUser(user);
    }

    @NotNull
    private IUser getiUser(User user) {
        return new IUser() {
            @Override
            public URI getSelf() {
                return user.getSelf();
            }

            @Override
            public String getName() {
                return user.getName();
            }

            @Override
            public String getDisplayName() {
                return user.getDisplayName();
            }

            @Override
            public String getAccoutId() {
                return user.getAccountId();
            }

            @Override
            public String getEmailAddress() {
                return user.getEmailAddress();
            }

            @Override
            public boolean isActive() {
                return user.isActive();
            }
        };
    }

    @Override
    public IUser getAssignee() {
        User user = issue.getAssignee();
        return getiUser(user);
    }

    @Override
    public String getSummary() {
        return issue.getSummary();
    }

    @Override
    public IBasicPriority getPriority() {
        BasicPriority prio = issue.getPriority();
        return new IBasicPriority() {
            @Override
            public URI getSelf() {
                return prio.getSelf();
            }

            @Override
            public String getName() {
                return prio.getName();
            }

            @Override
            public Long getId() {
                return prio.getId();
            }
        };
    }

    @Override
    public Iterable<IIssueLink> getIssueLinks() {
        List<IIssueLink> iLinks = new ArrayList<>();
        for (IssueLink link : issue.getIssueLinks()) {
            iLinks.add(new IIssueLink() {
                @Override
                public String getTargetIssueKey() {
                    return link.getTargetIssueKey();
                }

                @Override
                public URI getTargetIssueUri() {
                    return link.getTargetIssueUri();
                }

                @Override
                public IssueLinkType getIssueLinkType() {
                    return link.getIssueLinkType();
                }
            });
        }
        return iLinks;
    }

    @Override
    public Iterable<ISubtask> getSubtasks() {
        List<ISubtask> iTasks = new ArrayList<>();
        for (Subtask task : issue.getSubtasks()) {
            iTasks.add(new ISubtask() {
                @Override
                public String getIssueKey() {
                    return task.getIssueKey();
                }

                @Override
                public URI getIssueUri() {
                    return task.getIssueUri();
                }

                @Override
                public String getSummary() {
                    return task.getSummary();
                }

                @Override
                public IIssueType getIssueType() {
                    return getiIssueType(task.getIssueType());
                }

                @Override
                public IStatus getStatus() {
                    return getiStatus(task.getStatus());
                }
            });
        }
        return iTasks;
    }

    @Override
    public Iterable<IIssueField> getFields() {
        List<IIssueField> iIssueFields = new ArrayList<>();
        for (IssueField issueField : issue.getFields()) {
            iIssueFields.add(getiIssueField(issueField));
        }
        return iIssueFields;
    }

    @Override
    public IIssueField getField(String id) {
        IssueField issueField = issue.getField(id);
        return getiIssueField(issueField);
    }

    @NotNull
    private IIssueField getiIssueField(IssueField issueField) {
        return new IIssueField() {
            @Override
            public String getId() {
                return issueField.getId();
            }

            @Override
            public String getName() {
                return issueField.getName();
            }

            @Override
            public String getType() {
                return issueField.getType();
            }

            @Override
            public Object getValue() {
                return issueField.getValue();
            }
        };
    }

    @Override
    public IIssueField getFieldByName(String name) {
        IssueField issueField = issue.getFieldByName(name);
        return getiIssueField(issueField);
    }

    @Override
    public IIssueType getIssueType() {
        return getiIssueType(issue.getIssueType());
    }

    @NotNull
    private IIssueType getiIssueType(IssueType issueType) {
        return new IIssueType() {
            @Override
            public Long getId() {
                return issueType.getId();
            }

            @Override
            public String getName() {
                return issueType.getName();
            }

            @Override
            public boolean isSubtask() {
                return issueType.isSubtask();
            }

            @Override
            public URI getSelf() {
                return issueType.getSelf();
            }

            @Override
            public String getDescription() {
                return issueType.getDescription();
            }
        };
    }

    @Override
    public IBasicProject getProject() {
        BasicProject project = issue.getProject();
        return new IBasicProject() {
            @Override
            public URI getSelf() {
                return project.getSelf();
            }

            @Override
            public String getKey() {
                return project.getKey();
            }

            @Nullable
            @Override
            public String getName() {
                return project.getName();
            }

            @Nullable
            @Override
            public Long getId() {
                return project.getId();
            }
        };
    }

    @Override
    public IBasicVotes getVotes() {
        BasicVotes votes = issue.getVotes();
        return new IBasicVotes() {
            @Override
            public URI getSelf() {
                return votes.getSelf();
            }

            @Override
            public int getVotes() {
                return votes.getVotes();
            }

            @Override
            public boolean hasVoted() {
                return votes.hasVoted();
            }
        };
    }

    @Override
    public Iterable<IVersion> getFixVersions() {
        List<IVersion> iVersions = new ArrayList<>();
        for (Version version : issue.getFixVersions()) {
            iVersions.add(new IVersion() {
                @Override
                public URI getSelf() {
                    return version.getSelf();
                }

                @Nullable
                @Override
                public Long getId() {
                    return version.getId();
                }

                @Override
                public String getDescription() {
                    return version.getDescription();
                }

                @Override
                public String getName() {
                    return version.getName();
                }

                @Override
                public boolean isArchived() {
                    return version.isArchived();
                }

                @Override
                public boolean isReleased() {
                    return version.isReleased();
                }

                @Nullable
                @Override
                public DateTime getReleaseDate() {
                    return version.getReleaseDate();
                }
            });
        }
        return iVersions;
    }

    @Override
    public DateTime getCreationDate() {
        return issue.getCreationDate();
    }

    @Override
    public DateTime getUpdateDate() {
        return issue.getUpdateDate();
    }

    @Override
    public DateTime getDueDate() {
        return issue.getDueDate();
    }

    @Override
    public String getDescription() {
        return issue.getDescription();
    }

    @Override
    public String toString() {
        return "JiraArtifact [summary=" + getSummary() + ", description=" + getDescription() + ", self=" + getSelf() + ", key=" + getKey()
                + ", id=" + getId() + ", project=" + getProject() + ", issueType=" + getIssueType() + ", status=" + getStatus()
                + ", priority=" + getPriority() + ", reporter=" + getReporter() + ", assignee=" + getAssignee() + ", creationDate=" + getCreationDate()
                + ", updateDate=" + getUpdateDate() + ", dueDate=" + getDueDate() + "]";
    }
}
