package impactassessment.artifactconnector.jama;

import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import com.jamasoftware.services.restclient.jamadomain.values.*;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaProjectArtifact;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaRelease;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaUserArtifact;
import impactassessment.artifactconnector.jama.subtypes.JamaProjectArtifact;
import impactassessment.artifactconnector.jama.subtypes.JamaRelease;
import impactassessment.artifactconnector.jama.subtypes.JamaUserArtifact;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class JamaArtifact implements IJamaArtifact {

    private JamaItem jamaItem;
    private IJamaProjectArtifact jamaProjectArtifact;
    private IJamaUserArtifact userCreated;
    private IJamaUserArtifact userModified;

    public JamaArtifact(JamaItem jamaItem) {
        this.jamaItem = jamaItem;
        this.jamaProjectArtifact = new JamaProjectArtifact(jamaItem.getProject());
        this.userCreated = new JamaUserArtifact(jamaItem.getCreatedBy());
        this.userModified = new JamaUserArtifact(jamaItem.getModifiedBy());
    }

    @Override
    public Integer getId() {
        return jamaItem.getId();
    }

    @Override
    public boolean isProject() {
        return jamaItem.isProject();
    }

    @Override
    public String getName() {
        return jamaItem.getName().getValue();
    }

    @Override
    public String getGlobalId() {
        return jamaItem.getGlobalId();
    }

    @Override
    public IJamaProjectArtifact getProject() {
        return jamaProjectArtifact;
    }

    @Override
    public String getItemType() {
        return jamaItem.getItemType().getTypeKey();
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
        return jamaItem.getDocumentKey();
    }

    @Override
    public Date getCreatedDate() {
        return jamaItem.getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return jamaItem.getModifiedDate();
    }

    @Override
    public Date getLastActivityDate() {
        return jamaItem.getLastActivityDate();
    }

    @Override
    public List<IJamaArtifact> getChildren() throws RestClientException {
        return jamaItem.getChildren().stream()
                .map(JamaArtifact::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<IJamaArtifact> prefetchDownstreamItems() throws RestClientException {
        return jamaItem.prefetchDownstreamItems().stream()
                .map(JamaArtifact::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<IJamaArtifact> getDownstreamItems() {
        return jamaItem.getDownstreamItems().stream()
                .map(JamaArtifact::new)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<String> getStringValue(String fieldName) {
        JamaFieldValue jfv = jamaItem.getFieldValueByName(fieldName);
        if (jfv == null) {
            log.warn("Field name: {} not found", fieldName);
            return Optional.empty();
        }
        if (jfv instanceof CalculatedFieldValue) {
            return Optional.of((String) jfv.getValue());
        } else if (jfv instanceof TextFieldValue) {
            return Optional.of(((TextFieldValue) jfv).getValue());
        } else if (jfv instanceof URLFieldValue) {
            return Optional.of(((URLFieldValue) jfv).getValue());
        } else if (jfv instanceof TimeFieldValue) {
            return Optional.of((String) jfv.getValue());
        } else if (jfv instanceof TextBoxFieldValue) {
            return Optional.of(((TextBoxFieldValue) jfv).getValue());
        } else if (jfv instanceof TestCaseStatusFieldValue) {
            return Optional.of(((TestCaseStatusFieldValue) jfv).getValue());
        } else if (jfv instanceof RichTextFieldValue) {
            return Optional.of(((RichTextFieldValue) jfv).getValue().getValue());
        } else {
            log.warn("Invalid field value type: {}", jfv.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Date> getDateValue(String fieldName) {
        JamaFieldValue jfv = jamaItem.getFieldValueByName(fieldName);
        if (jfv instanceof DateFieldValue) {
            return Optional.of((Date) jfv.getValue());
        } else {
            log.warn("Invalid field value type: {}", jfv.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> getBooleanValue(String fieldName) {
        JamaFieldValue jfv = jamaItem.getFieldValueByName(fieldName);
        if (jfv instanceof FlagFieldValue) {
            return Optional.of((Boolean) jfv.getValue());
        } else {
            log.warn("Invalid field value type: {}", jfv.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> getIntegerValue(String fieldName) {
        JamaFieldValue jfv = jamaItem.getFieldValueByName(fieldName);
        if (jfv instanceof IntegerFieldValue) {
            return Optional.of((Integer) jfv.getValue());
        } else if (jfv instanceof RollupFieldValue) {
            return Optional.of(((RollupFieldValue) jfv).getValue());
        } else {
            log.warn("Invalid field value type: {}", jfv.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<IJamaProjectArtifact> getJamaProjectValue(String fieldName) {
        JamaFieldValue jfv = jamaItem.getFieldValueByName(fieldName);
        if (jfv instanceof ProjectFieldValue) {
            return Optional.of(new JamaProjectArtifact(((ProjectFieldValue) jfv).getValue()));
        } else {
            log.warn("Invalid field value type: {}", jfv.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<IJamaRelease> getJamaReleaseValue(String fieldName) {
        JamaFieldValue jfv = jamaItem.getFieldValueByName(fieldName);
        if (jfv instanceof ReleaseFieldValue) {
            return Optional.of(new JamaRelease(((ReleaseFieldValue) jfv).getValue()));
        } else {
            log.warn("Invalid field value type: {}", jfv.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<IJamaUserArtifact> getJamaUserValue(String fieldName) {
        JamaFieldValue jfv = jamaItem.getFieldValueByName(fieldName);
        if (jfv instanceof UserFieldValue) {
            return Optional.of(new JamaUserArtifact(((UserFieldValue) jfv).getValue()));
        } else {
            log.warn("Invalid field value type: {}", jfv.getClass().getSimpleName());
            return Optional.empty();
        }
    }



}
