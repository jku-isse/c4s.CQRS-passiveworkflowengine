package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.ArtifactInput;
import impactassessment.passiveprocessengine.instance.ArtifactOutput;

import java.util.List;

public interface IInputOutputArtifact {

    List<ArtifactOutput> getOutput();
    boolean removeOutput(ArtifactOutput ao);
    void addOutput(ArtifactOutput ao);

    List<ArtifactInput> getInput();
    boolean removeInput(ArtifactInput ai);
    void addInput(ArtifactInput ai);
}
