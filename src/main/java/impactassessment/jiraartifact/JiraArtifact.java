package impactassessment.jiraartifact;

import com.atlassian.jira.rest.client.api.StatusCategory;
import com.atlassian.jira.rest.client.api.domain.*;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class JiraArtifact implements IJiraArtifact {

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
    public IJiraStatus getStatus() {
        return getiStatus(issue.getStatus());
    }

    @NotNull
    private IJiraStatus getiStatus(Status status) {
        return new IJiraStatus() {
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
            public IJiraStatusCategory getStatusCategory() {
                StatusCategory statusCategory = status.getStatusCategory();
                return new IJiraStatusCategory() {
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
    public IJiraUser getReporter() {
        User user = issue.getReporter();
        return getiUser(user);
    }

    @NotNull
    private IJiraUser getiUser(User user) {
        return new IJiraUser() {
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
    public IJiraUser getAssignee() {
        User user = issue.getAssignee();
        return getiUser(user);
    }

    @Override
    public String getSummary() {
        return issue.getSummary();
    }

    @Override
    public IJiraBasicPriority getPriority() {
        BasicPriority prio = issue.getPriority();
        if (prio == null) return null;
        return new IJiraBasicPriority() {
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
    public Iterable<IJiraIssueLink> getIssueLinks() {
        List<IJiraIssueLink> iLinks = new ArrayList<>();
        for (IssueLink link : issue.getIssueLinks()) {
            iLinks.add(new IJiraIssueLink() {
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
    public Iterable<IJiraSubtask> getSubtasks() {
        List<IJiraSubtask> iTasks = new ArrayList<>();
        for (Subtask task : issue.getSubtasks()) {
            iTasks.add(new IJiraSubtask() {
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
                public IJiraIssueType getIssueType() {
                    return getiIssueType(task.getIssueType());
                }

                @Override
                public IJiraStatus getStatus() {
                    return getiStatus(task.getStatus());
                }
            });
        }
        return iTasks;
    }

    @Override
    public Iterable<IJiraIssueField> getFields() {
        List<IJiraIssueField> iIssueFields = new ArrayList<>();
        for (IssueField issueField : issue.getFields()) {
            iIssueFields.add(getiIssueField(issueField));
        }
        return iIssueFields;
    }

    @Override
    public IJiraIssueField getField(String id) {
        IssueField issueField = issue.getField(id);
        return getiIssueField(issueField);
    }

    @NotNull
    private IJiraIssueField getiIssueField(IssueField issueField) {
        return new IJiraIssueField() {
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
    public IJiraIssueField getFieldByName(String name) {
        IssueField issueField = issue.getFieldByName(name);
        return getiIssueField(issueField);
    }

    @Override
    public IJiraIssueType getIssueType() {
        return getiIssueType(issue.getIssueType());
    }

    @NotNull
    private IJiraIssueType getiIssueType(IssueType issueType) {
        return new IJiraIssueType() {
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
    public IJiraBasicProject getProject() {
        BasicProject project = issue.getProject();
        return new IJiraBasicProject() {
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
    public IJiraBasicVotes getVotes() {
        BasicVotes votes = issue.getVotes();
        return new IJiraBasicVotes() {
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
    public Iterable<IJiraVersion> getFixVersions() {
        List<IJiraVersion> iVersions = new ArrayList<>();
        for (Version version : issue.getFixVersions()) {
            iVersions.add(new IJiraVersion() {
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
