package impactassessment.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.jama.IJamaArtifact;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.junit.Assert.*;

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
        Optional<IArtifact> artifact = jamaService.get(ai, "irrelevant for this test");
        assertTrue(artifact.isPresent());
        assertTrue(artifact.get() instanceof IJamaArtifact);
        System.out.println(artifact);
    }

    @Test
    public void fetchJamaArtifactViaRegistry() {
        ArtifactIdentifier ai = new ArtifactIdentifier("10071184", "IJamaArtifact");
        Optional<IArtifact> artifact = registry.get(ai, "irrelevant for this test");
        assertTrue(artifact.isPresent());
        assertTrue(artifact.get() instanceof IJamaArtifact);
        System.out.println(artifact);
    }

}
