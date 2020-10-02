package impactassessment.passiveprocessengine.persistance;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import impactassessment.passiveprocessengine.definition.DecisionNodeDefinition;
import impactassessment.passiveprocessengine.definition.DefaultWorkflowDefinition;
import impactassessment.passiveprocessengine.definition.IBranchDefinition;
import impactassessment.passiveprocessengine.definition.TaskDefinition;

import java.io.IOException;

public class MultiTypeAdapterFactory  implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() == IBranchDefinition.class) {
            return (TypeAdapter<T>) gson.getAdapter(PersistableBranchDefinition.class);
        }
        if (type.getRawType() == DecisionNodeDefinition.class) {
            return (TypeAdapter<T>) wrapDecisionNodeAdapter(gson, new TypeToken<DecisionNodeDefinition>() {});
        }
        if (type.getRawType() == DefaultWorkflowDefinition.class) {
            return (TypeAdapter<T>) wrapWorkflowDefinitionAdapter(gson, new TypeToken<DefaultWorkflowDefinition>() {});
        }
        if (type.getRawType() == TaskDefinition.class) {
            return (TypeAdapter<T>) wrapTaskDefinitionAdapter(gson, new TypeToken<TaskDefinition>() {});
        }

        return null;
    }

    private TypeAdapter<TaskDefinition> wrapTaskDefinitionAdapter(Gson gson, TypeToken<TaskDefinition> type) {
        final TypeAdapter<TaskDefinition> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<TaskDefinition>() {

            @Override
            public void write(JsonWriter out, TaskDefinition value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public TaskDefinition read(JsonReader in) throws IOException {
                TaskDefinition td = delegate.read(in);
                ShortTermTaskDefinitionCache.addToCache(td);
                return td;
            }

        };
    }

    private TypeAdapter<DefaultWorkflowDefinition> wrapWorkflowDefinitionAdapter(Gson gson, TypeToken<DefaultWorkflowDefinition> type) {
        final TypeAdapter<DefaultWorkflowDefinition> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<DefaultWorkflowDefinition>() {

            @Override
            public void write(JsonWriter out, DefaultWorkflowDefinition value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public DefaultWorkflowDefinition read(JsonReader in) throws IOException {
                DefaultWorkflowDefinition wfd = delegate.read(in);
                wfd.propagateWorkflowDefinitionId();
                return wfd;
            }

        };
    }

    private TypeAdapter<DecisionNodeDefinition> wrapDecisionNodeAdapter(Gson gson, TypeToken<DecisionNodeDefinition> type) {
        final TypeAdapter<DecisionNodeDefinition> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<DecisionNodeDefinition>() {

            @Override
            public void write(JsonWriter out, DecisionNodeDefinition value) throws IOException {
                delegate.write(out, value);
            }

            @Override public DecisionNodeDefinition read(JsonReader in) throws IOException {
                DecisionNodeDefinition dnd = delegate.read(in);
                dnd.getInBranches().stream()
                        .filter(PersistableBranchDefinition.class::isInstance)
                        .map(PersistableBranchDefinition.class::cast)
                        .forEach(bd -> bd.setDecisionNodeDefinition(dnd));
                dnd.getOutBranches().stream()
                        .filter(PersistableBranchDefinition.class::isInstance)
                        .map(PersistableBranchDefinition.class::cast)
                        .forEach(bd -> bd.setDecisionNodeDefinition(dnd));
                return dnd;
            }
        };
    }

}
