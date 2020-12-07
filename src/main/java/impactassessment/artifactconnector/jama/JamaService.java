package impactassessment.artifactconnector.jama;

import c4s.jamaconnector.IJamaChangeSubscriber;
import c4s.jamaconnector.JamaConnector;
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

    private JamaConnector jamaConn;
    private IJamaChangeSubscriber jamaChangeSubscriber;

    public JamaService(JamaConnector jamaConn, IJamaChangeSubscriber jamaChangeSubscriber) {
        this.jamaConn = jamaConn;
        this.jamaChangeSubscriber = jamaChangeSubscriber;
    }

    @Override
    public boolean provides(String type) {
        return type.equals(TYPE);
    }

    @Override
    public IArtifact get(ArtifactIdentifier id, String workflowId) {
        JamaItem jamaItem = jamaConn.getJamaItemAndItsJiraKey(Integer.parseInt(id.getId())).getKey();
        return new JamaArtifact(jamaItem);
    }

}
