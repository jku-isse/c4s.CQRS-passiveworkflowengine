package impactassessment.artifactconnector.jama;

import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.LazyBase;
import com.jamasoftware.services.restclient.jamadomain.values.*;
import impactassessment.SpringUtil;
import impactassessment.artifactconnector.ArtifactIdentifier;
import impactassessment.artifactconnector.IArtifact;
import impactassessment.artifactconnector.IArtifactRegistry;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaProjectArtifact;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaRelease;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaUserArtifact;
import impactassessment.artifactconnector.jama.subtypes.JamaProjectArtifact;
import impactassessment.artifactconnector.jama.subtypes.JamaRelease;
import impactassessment.artifactconnector.jama.subtypes.JamaUserArtifact;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JamaArtifact implements IJamaArtifact {
    // id for axon application
    private ArtifactIdentifier artifactIdentifier;
    // fields of jama item
    private Integer id;
    private Boolean isProject;
    private String name;
    private String globalId;
    private String itemType;
    private String documentKey;
    private Date createdDate;
    private Date modifiedDate;
    private Date lastActivityDate;

    private List<String> children;
    private List<String> prefetchItems;
    private List<String> downstreamItems;

    private Map<String, String> stringValues = new HashMap<>();
    private Map<String, Date> dateValues = new HashMap<>();
    private Map<String, Integer> intValues = new HashMap<>();
    private Map<String, Boolean> booleanValues = new HashMap<>();
    private Map<String, IJamaProjectArtifact> projectValues = new HashMap<>();
    private Map<String, IJamaRelease> releaseValues = new HashMap<>();
    private Map<String, IJamaUserArtifact> userValues = new HashMap<>();

    private IJamaProjectArtifact jamaProjectArtifact;
    private IJamaUserArtifact userCreated;
    private IJamaUserArtifact userModified;

    private transient IArtifactRegistry artifactRegistry = null;

    public JamaArtifact(JamaItem jamaItem) {
        // id for axon application
        this.artifactIdentifier = new ArtifactIdentifier(String.valueOf(jamaItem.getId()), IJamaArtifact.class.getSimpleName());
        // simple fields of jama item
        this.id = jamaItem.getId();
        this.isProject = jamaItem.isProject();
        this.name = jamaItem.getName() == null ? null : jamaItem.getName().getValue();
        this.globalId = jamaItem.getGlobalId();
        this.itemType = jamaItem.getItemType() == null ? null : jamaItem.getItemType().getTypeKey();
        this.documentKey = jamaItem.getDocumentKey();
        this.createdDate = jamaItem.getCreatedDate();
        this.modifiedDate = jamaItem.getModifiedDate();
        this.lastActivityDate = jamaItem.getLastActivityDate();
//        try {
//            this.children = jamaItem.getChildren().stream()
//                    .map(LazyBase::getId)
//                    .map(String::valueOf)
//                    .collect(Collectors.toList());
//            this.prefetchItems = jamaItem.prefetchDownstreamItems().stream()
//                    .map(LazyBase::getId)
//                    .map(String::valueOf)
//                    .collect(Collectors.toList());
//        } catch (RestClientException e) {
//            e.printStackTrace();
//        }
//        this.downstreamItems = jamaItem.getDownstreamItems().stream()
//                .map(LazyBase::getId)
//                .map(String::valueOf)
//                .collect(Collectors.toList());

        for (JamaFieldValue jfv : jamaItem.getFieldValues()) {
            // TODO: Performance issue: if value was present once, the remaining checks should be skipped
            getString(jfv).ifPresent(s -> stringValues.put(jfv.getName(), s));
            getInt(jfv).ifPresent(i -> intValues.put(jfv.getName(), i));
            getBoolean(jfv).ifPresent(b -> booleanValues.put(jfv.getName(), b));
            getDate(jfv).ifPresent(d -> dateValues.put(jfv.getName(), d));
            getProject(jfv).ifPresent(p -> projectValues.put(jfv.getName(), p));
            getUser(jfv).ifPresent(u -> userValues.put(jfv.getName(), u));
            getRelease(jfv).ifPresent(r -> releaseValues.put(jfv.getName(), r));
        }
        // complex field of jama item
        this.jamaProjectArtifact = new JamaProjectArtifact(jamaItem.getProject());
        this.userCreated = new JamaUserArtifact(jamaItem.getCreatedBy());
        this.userModified = new JamaUserArtifact(jamaItem.getModifiedBy());
    }

    private JamaArtifact fetch(String artifactId, String workflowId) {
        log.info("Artifact fetching linked item: {}", artifactId);
        if (artifactRegistry == null)
            artifactRegistry = SpringUtil.getBean(IArtifactRegistry.class);
        ArtifactIdentifier ai = new ArtifactIdentifier(artifactId, IJamaArtifact.class.getSimpleName());
        IArtifact a = artifactRegistry.get(ai, workflowId);
        return (JamaArtifact) a;
    }

    @Override
    public ArtifactIdentifier getArtifactIdentifier() {
        return artifactIdentifier;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean isProject() {
        return isProject;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGlobalId() {
        return globalId;
    }

    @Override
    public IJamaProjectArtifact getProject() {
        return jamaProjectArtifact;
    }

    @Override
    public String getItemType() {
        return itemType;
    }

    @Override
    public IJamaUserArtifact getCreatedBy() {
        return userCreated;
    }

    @Override
    public IJamaUserArtifact getModifiedBy() {
        return userModified;
    }

    @Override
    public String getDocumentKey() {
        return documentKey;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public Date getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public Date getLastActivityDate() {
        return lastActivityDate;
    }

    @Override
    public List<IJamaArtifact> getChildren(String workflowId) {
        return children.stream()
                .map(child -> fetch(child, workflowId))
                .collect(Collectors.toList());
    }

    @Override
    public List<IJamaArtifact> prefetchDownstreamItems(String workflowId) {
        return prefetchItems.stream()
                .map(child -> fetch(child, workflowId))
                .collect(Collectors.toList());
    }

    @Override
    public List<IJamaArtifact> getDownstreamItems(String workflowId) {
        return downstreamItems.stream()
                .map(child -> fetch(child, workflowId))
                .collect(Collectors.toList());
    }

    @Override
    public String getStringValue(String fieldName) {
        return stringValues.get(fieldName);
    }

    @Override
    public Date getDateValue(String fieldName) {
        return dateValues.get(fieldName);
    }

    @Override
    public Boolean getBooleanValue(String fieldName) {
        return booleanValues.get(fieldName);
    }

    @Override
    public Integer getIntegerValue(String fieldName) {
        return intValues.get(fieldName);
    }

    @Override
    public IJamaProjectArtifact getJamaProjectValue(String fieldName) {
        return projectValues.get(fieldName);
    }

    @Override
    public IJamaRelease getJamaReleaseValue(String fieldName) {
        return releaseValues.get(fieldName);
    }

    @Override
    public IJamaUserArtifact getJamaUserValue(String fieldName) {
        return userValues.get(fieldName);
    }

    @Override
    public String toString() {
        return "JamaArtifact{" +
                "documentKey=" + getDocumentKey() +
                ", artifactIdentifier=" + artifactIdentifier +
                ", createdBy=" + getCreatedBy() +
                ", createdDate=" + getCreatedDate() +
                ", globalId=" + getGlobalId() +
                ", id=" + getId() +
                ", itemType=" + getItemType() +
                ", lastActivityDate=" + getLastActivityDate() +
                ", modifiedBy=" + getModifiedBy() +
                ", modifiedDate=" + getModifiedDate() +
                ", name=" + getName() +
                ", project=" + getProject() +
                '}';
    }

    private Optional<String> getString(JamaFieldValue jfv) {
        if (jfv instanceof CalculatedFieldValue) {
            return Optional.ofNullable((String) jfv.getValue());
        } else if (jfv instanceof TextFieldValue) {
            return Optional.ofNullable(((TextFieldValue) jfv).getValue());
        } else if (jfv instanceof URLFieldValue) {
            return Optional.ofNullable(((URLFieldValue) jfv).getValue());
        } else if (jfv instanceof TimeFieldValue) {
            return Optional.ofNullable((String) jfv.getValue());
        } else if (jfv instanceof TextBoxFieldValue) {
            return Optional.ofNullable(((TextBoxFieldValue) jfv).getValue());
        } else if (jfv instanceof TestCaseStatusFieldValue) {
            return Optional.ofNullable(((TestCaseStatusFieldValue) jfv).getValue());
        } else if (jfv instanceof RichTextFieldValue) {
            return Optional.ofNullable(((RichTextFieldValue) jfv).getValue().getValue());
        } else {
            return Optional.empty();
        }
    }

    private Optional<Date> getDate(JamaFieldValue jfv) {
        if (jfv instanceof DateFieldValue) {
            return Optional.ofNullable((Date) jfv.getValue());
        } else {
            return Optional.empty();
        }
    }

    private Optional<Integer> getInt(JamaFieldValue jfv) {
        if (jfv instanceof IntegerFieldValue) {
            return Optional.ofNullable((Integer) jfv.getValue());
        } else if (jfv instanceof RollupFieldValue) {
            return Optional.ofNullable(((RollupFieldValue) jfv).getValue());
        } else {
            return Optional.empty();
        }
    }

    private Optional<Boolean> getBoolean(JamaFieldValue jfv) {
        if (jfv instanceof FlagFieldValue) {
            return Optional.ofNullable((Boolean) jfv.getValue());
        } else {
            return Optional.empty();
        }
    }

    private Optional<IJamaProjectArtifact> getProject(JamaFieldValue jfv) {
        if (jfv instanceof ProjectFieldValue) {
            return Optional.of(new JamaProjectArtifact(((ProjectFieldValue) jfv).getValue()));
        } else {
            return Optional.empty();
        }
    }

    private Optional<IJamaRelease> getRelease(JamaFieldValue jfv) {
        if (jfv instanceof ReleaseFieldValue) {
            return Optional.of(new JamaRelease(((ReleaseFieldValue) jfv).getValue()));
        } else {
            return Optional.empty();
        }
    }

    private Optional<IJamaUserArtifact> getUser(JamaFieldValue jfv) {
        if (jfv instanceof UserFieldValue) {
            return Optional.of(new JamaUserArtifact(((UserFieldValue) jfv).getValue()));
        } else {
            return Optional.empty();
        }
    }
}
