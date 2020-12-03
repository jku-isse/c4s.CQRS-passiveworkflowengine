package impactassessment.artifactconnector.jama;

import c4s.jamaconnector.IJamaChangeSubscriber;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import impactassessment.artifactconnector.ArtifactIdentifier;
import impactassessment.artifactconnector.IArtifact;
import impactassessment.artifactconnector.IArtifactService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JamaService implements IArtifactService {

    private static final String TYPE = IJamaArtifact.class.getSimpleName();

    private JamaInstance jamaInstance;
    private IJamaChangeSubscriber jamaChangeSubscriber;

    public JamaService(JamaInstance jamaInstance, IJamaChangeSubscriber jamaChangeSubscriber) {
        this.jamaInstance = jamaInstance;
        this.jamaChangeSubscriber = jamaChangeSubscriber;
    }

    @Override
    public boolean provides(String type) {
        return type.equals(TYPE);
    }

    @Override
    public IArtifact get(ArtifactIdentifier id, String workflowId) {
        try {
            JamaItem jamaItem = jamaInstance.getItem(Integer.parseInt(id.getId()));
            return new JamaArtifact(jamaItem);
        } catch (RestClientException e) {
            log.error("RestClientException");
            return null;
        }
    }

}
