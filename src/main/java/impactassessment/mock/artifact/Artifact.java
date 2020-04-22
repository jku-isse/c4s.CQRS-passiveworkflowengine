package impactassessment.mock.artifact;

import java.util.HashMap;
import java.util.Map;

public class Artifact {
    private Map<String,String> fields;
    public Artifact() {
        fields = new HashMap<>();
    }
    public String getId() {
        return fields.get("id");
    }
    public String getField(String fieldName) {
        return fields.get(fieldName);
    }
    public void setField(String fieldName, String fieldValue) {
        fields.put(fieldName, fieldValue);
    }
}
