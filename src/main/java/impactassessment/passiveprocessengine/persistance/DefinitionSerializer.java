package impactassessment.passiveprocessengine.persistance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import impactassessment.passiveprocessengine.definition.DefaultWorkflowDefinition;
import impactassessment.passiveprocessengine.definition.WorkflowDefinition;

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

}
