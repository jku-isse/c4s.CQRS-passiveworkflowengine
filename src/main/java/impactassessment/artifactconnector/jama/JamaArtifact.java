package impactassessment.artifactconnector.jama;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import com.jamasoftware.services.restclient.jamadomain.values.*;
import impactassessment.artifactconnector.jama.subtypes.JamaProjectArtifact;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

@Slf4j
public class JamaArtifact implements IJamaArtifact {

    private JamaItem jamaItem;

    public JamaArtifact(JamaItem jamaItem) {
        this.jamaItem = jamaItem;
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
    public Optional<JamaProjectArtifact> getJamaProject(String fieldName) {
        JamaFieldValue jfv = jamaItem.getFieldValueByName(fieldName);
        if (jfv instanceof ProjectFieldValue) {
            return Optional.of(new JamaProjectArtifact(((ProjectFieldValue) jfv).getValue()));
        } else {
            log.warn("Invalid field value type: {}", jfv.getClass().getSimpleName());
            return Optional.empty();
        }
    }

}
