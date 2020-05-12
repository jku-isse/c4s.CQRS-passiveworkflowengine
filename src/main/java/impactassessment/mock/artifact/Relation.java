package impactassessment.mock.artifact;

import lombok.Getter;

public class Relation {
    private @Getter Artifact source;
    private @Getter Artifact destination;

    private @Getter String sourceRole;
    private @Getter String destinationRole;
}
