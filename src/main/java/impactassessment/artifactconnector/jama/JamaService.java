package impactassessment.artifactconnector.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.jama.IJamaArtifact;
import c4s.jamaconnector.IJamaChangeSubscriber;
import c4s.jamaconnector.JamaConnector;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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
    public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
        JamaItem jamaItem = jamaConn.getJamaItemAndItsJiraKey(Integer.parseInt(id.getId())).getKey();
        if (jamaItem != null) {
            // TODO add artifact usage to jamaChangeSubscriber
            return Optional.of(new JamaArtifact(jamaItem));
        } else {
            return Optional.empty();
        }
    }

}
