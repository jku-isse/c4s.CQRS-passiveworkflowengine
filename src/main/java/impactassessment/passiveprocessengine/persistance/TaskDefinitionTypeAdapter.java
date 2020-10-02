package impactassessment.passiveprocessengine.persistance;

import java.io.IOException;
import java.util.HashMap;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import impactassessment.passiveprocessengine.definition.TaskDefinition;

public class TaskDefinitionTypeAdapter extends TypeAdapter<TaskDefinition> {

//	private HashMap<String, TaskDefinition> defs = new HashMap<>();
//
//	public TaskDefinitionTypeAdapter(HashMap<String, TaskDefinition> defs) {
//		this.defs=defs;
//	}

    public TaskDefinitionTypeAdapter() {}

    @Override
    public void write(JsonWriter out, TaskDefinition value) throws IOException {
        if (value == null || value.getId() == null)
            out.nullValue();
        else {
            out.value(value.getId());
        }
    }

    @Override
    public TaskDefinition read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        String id = reader.nextString();
        return ShortTermTaskDefinitionCache.getFromCache(id);
    }

}
