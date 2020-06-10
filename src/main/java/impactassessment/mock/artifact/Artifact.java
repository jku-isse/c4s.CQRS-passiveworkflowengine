package impactassessment.mock.artifact;


import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Artifact {



    private Map<String,String> fields;
    private @Getter List<Relation> relationsIncoming;
    private @Getter List<Relation> relationsOutgoing;

    public Artifact() {
        fields = new HashMap<>();
        relationsIncoming = new ArrayList<>();
        relationsOutgoing = new ArrayList<>();
    }
    public String getId() {
        return fields.get(MockService.ID);
    }
    public String getField(String fieldName) {
        return fields.get(fieldName);
    }
    public void setField(String fieldName, String fieldValue) {
        fields.put(fieldName, fieldValue);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        fields.entrySet().stream().forEach(entry -> builder.append(entry.getKey() + ": " + entry.getValue() + ", "));
        return "Artifact ["+builder+"]";
    }
}
