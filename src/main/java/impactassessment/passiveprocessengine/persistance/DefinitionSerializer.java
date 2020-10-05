package impactassessment.passiveprocessengine.persistance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import impactassessment.passiveprocessengine.definition.DefaultWorkflowDefinition;
import impactassessment.passiveprocessengine.definition.WorkflowDefinition;

import java.io.FileReader;

public class DefinitionSerializer {

    Gson gson;

    public DefinitionSerializer() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new MultiTypeAdapterFactory())
                .setPrettyPrinting()
                .create();
    }



    public String toJson(WorkflowDefinition wfd) {
        return gson.toJson(wfd);
    }


    public WorkflowDefinition fromJson(String wfdJson) throws JsonSyntaxException {
        WorkflowDefinition wfd = gson.fromJson(wfdJson, DefaultWorkflowDefinition.class);
        return wfd;
    }

    public WorkflowDefinition fromJson(FileReader wfdJson) throws JsonSyntaxException {
        JsonReader reader = new JsonReader(wfdJson);
        WorkflowDefinition wfd = gson.fromJson(reader, DefaultWorkflowDefinition.class);
        return wfd;
    }

}
