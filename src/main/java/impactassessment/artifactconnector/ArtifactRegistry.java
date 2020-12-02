package impactassessment.artifactconnector;

import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.instance.ArtifactWrapper;

import java.util.Collection;

@Slf4j
public class ArtifactRegistry implements IArtifactRegistry {

    private Collection<IArtifactService> services;

    @Override
    public ArtifactWrapper get(ArtifactIdentifier id, String workflowId) {
        for (IArtifactService service : services) {
            if (service.provides(id.getType())) {
                IArtifact a = service.get(id, workflowId);
                return new ArtifactWrapper(id.getId(), id.getType(), null, a);
            }
        }
        log.warn("No service registered that provides artifacts of type {}", id.getType());
        return null;
    }

    @Override
    public void register(IArtifactService service) {
        services.add(service);
    }

}
