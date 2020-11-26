package impactassessment.artifactconnector;

import passiveprocessengine.instance.ArtifactWrapper;

public interface IArtifactRegistry {

    ArtifactWrapper get(String type, String idType, String id);

    ArtifactWrapper get(String type, String id);

}
