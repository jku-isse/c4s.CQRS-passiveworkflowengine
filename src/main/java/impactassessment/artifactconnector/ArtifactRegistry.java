package impactassessment.artifactconnector;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Slf4j
public class ArtifactRegistry implements IArtifactRegistry {

    private Collection<IArtifactService> services = new ArrayList<>();

    @Override
    public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
        for (IArtifactService service : services) {
            if (service.provides(id.getType())) {
                return service.get(id, workflowId);
            }
        }
        log.warn("No service registered that provides artifacts of type {}", id.getType());
        return null; //TODO Optional
    }

    @Override
    public void register(IArtifactService service) {
        services.add(service);
    }

}
