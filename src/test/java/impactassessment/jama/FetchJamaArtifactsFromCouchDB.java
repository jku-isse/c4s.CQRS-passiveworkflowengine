package impactassessment.jama;

import impactassessment.artifactconnector.ArtifactIdentifier;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.IArtifact;
import impactassessment.artifactconnector.jama.IJamaArtifact;
import impactassessment.artifactconnector.jama.JamaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OnlyJamaConfig.class)
public class FetchJamaArtifactsFromCouchDB {
    @Autowired
    private JamaService jamaService;

    private ArtifactRegistry registry;

    @Before
    public void setup() {
        registry = new ArtifactRegistry();
        registry.register(jamaService);
    }

    @Test
    public void fetchJamaArtifact() {
        ArtifactIdentifier ai = new ArtifactIdentifier("1562790", "IJamaArtifact");
        IArtifact artifact = jamaService.get(ai, "irrelevant for this test");
        assertNotNull(artifact);
        assertTrue(artifact instanceof IJamaArtifact);
        System.out.println(artifact);
    }

    @Test
    public void fetchJamaArtifactViaRegistry() {
        ArtifactIdentifier ai = new ArtifactIdentifier("10071184", "IJamaArtifact");
        IArtifact artifact = registry.get(ai, "irrelevant for this test");
        assertNotNull(artifact);
        assertTrue(artifact instanceof IJamaArtifact);
        System.out.println(artifact);
    }

}
