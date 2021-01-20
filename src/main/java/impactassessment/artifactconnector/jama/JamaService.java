package impactassessment.artifactconnector.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.jama.IJamaArtifact;
import c4s.jamaconnector.JamaConnector;
import com.jamasoftware.services.restclient.exception.JsonException;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class JamaService implements IArtifactService {

    private static final String TYPE = IJamaArtifact.class.getSimpleName();

    private JamaInstance jamaInstance;
    private JamaChangeSubscriber jamaChangeSubscriber;

    public JamaService(JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        this.jamaInstance = jamaInstance;
        this.jamaChangeSubscriber = jamaChangeSubscriber;
    }

    @Override
    public boolean provides(String type) {
        return type.equals(TYPE);
    }

    @Override
    public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
        JamaItem jamaItem;
        try {
            jamaItem = jamaInstance.getItem(Integer.parseInt(id.getId()));
        } catch (Exception e) {
            log.error("Jama Item could not be retrieved: "+e.getClass().getSimpleName());
            return Optional.empty();
        }
        if (jamaItem != null) {
            jamaChangeSubscriber.addUsage(workflowId, id);
            return Optional.of(new JamaArtifact(jamaItem));
        } else {
            return Optional.empty();
        }
    }

    public String getJamaServerUrl(JamaItem jamaItem) {
        return jamaInstance.getOpenUrl(jamaItem);
    }
}
