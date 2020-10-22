package impactassessment.passiveprocessengine.persistance;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import impactassessment.passiveprocessengine.definition.*;

import java.io.IOException;
import java.util.Map;

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
            return (TypeAdapter<T>) wrapTaskDefinitionAdapter(gson);
        }

        return null;
    }

    private TypeAdapter<TaskDefinition> wrapTaskDefinitionAdapter(Gson gson) {
        final TypeAdapter<TaskDefinition> tdDelegate = gson.getDelegateAdapter(this, new TypeToken<TaskDefinition>() {});
        final TypeAdapter<NoOpTaskDefinition> noOpDelegate = gson.getDelegateAdapter(this, new TypeToken<NoOpTaskDefinition>() {});
        final TypeAdapter<WorkflowWrapperTaskDefinition> wfWrapDelegate = gson.getDelegateAdapter(this, new TypeToken<WorkflowWrapperTaskDefinition>() {});

        final TypeAdapter<TaskDefinitionIntermediary> intermediaryDelegate = gson.getDelegateAdapter(this, new TypeToken<TaskDefinitionIntermediary>() {});

        return new TypeAdapter<TaskDefinition>() {

            @Override
            public void write(JsonWriter out, TaskDefinition value) throws IOException {
                if (value instanceof NoOpTaskDefinition) {
                    noOpDelegate.write(out, (NoOpTaskDefinition) value);
                } else if (value instanceof WorkflowWrapperTaskDefinition) {
                    wfWrapDelegate.write(out, (WorkflowWrapperTaskDefinition) value);
                } else {
                    tdDelegate.write(out, value);
                }
            }

            @Override
            public TaskDefinition read(JsonReader in) throws IOException {
                TaskDefinitionIntermediary intermediary = intermediaryDelegate.read(in);
                TaskDefinition td;
                if (intermediary.isNoOp()) {
                    td = new NoOpTaskDefinition(intermediary.getId(), null);

                } else if (intermediary.getSubWfdId() != null) {
                    td = new WorkflowWrapperTaskDefinition(intermediary.getId(), null, intermediary.getSubWfdId());
                } else {
                    td = new TaskDefinition(intermediary.getId(), null);
                }

                for (Map.Entry<String, ArtifactType> e : intermediary.getExpectedInput().entrySet()) {
                    td.putExpectedInput(e.getKey(), e.getValue());
                }
                for (Map.Entry<String, ArtifactType> e : intermediary.getExpectedOutput().entrySet()) {
                    td.putExpectedOutput(e.getKey(), e.getValue());
                }

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
