package impactassessment.passiveprocessengine.definition;

import java.util.Map;

public interface ITaskDefinition extends IdentifiableObject {
    Map<String,ArtifactType> getExpectedInput();

    ArtifactType putExpectedInput(String key, ArtifactType value);

    Map<String,ArtifactType> getExpectedOutput();

    ArtifactType putExpectedOutput(String key, ArtifactType value);
}
