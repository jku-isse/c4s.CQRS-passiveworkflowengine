package impactassessment.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.jama.IJamaArtifact;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaArtifact;
import impactassessment.artifactconnector.jama.JamaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.util.*;

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
        ArtifactIdentifier ai = new ArtifactIdentifier("PVCSG-SRS-20294", "IJamaArtifact");
        Optional<IArtifact> artifact = jamaService.get(ai, "irrelevant for this test");
        assertTrue(artifact.isPresent());
        assertTrue(artifact.get() instanceof IJamaArtifact);
        System.out.println(artifact);
    }

    @Test
    public void fetchJamaArtifactViaRegistry() {
        ArtifactIdentifier ai = new ArtifactIdentifier("PVCSG-SRS-20294", "IJamaArtifact");
        Optional<IArtifact> artifact = registry.get(ai, "irrelevant for this test");
        assertTrue(artifact.isPresent());
        assertTrue(artifact.get() instanceof IJamaArtifact);
        System.out.println(artifact);
    }

    @Test
    public void fetchAllItems() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File("C:\\Users\\stefan\\Desktop\\AllIntegerIds.txt")));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            ArtifactIdentifier ai = new ArtifactIdentifier(line, "IJamaArtifact");
            Optional<IArtifact> opt = registry.get(ai, "irrelevant for this test");
            assertTrue(opt.isPresent());
            JamaArtifact artifact = (JamaArtifact) opt.get();
            if (artifact.getDownstreamItemIds().size() > 0) {
                System.out.println("fetched");
            }
        }
        System.out.println("done");
    }

//    @Test
//    public void fetchAllWPsAndCountSRSUsages() throws IOException {
//        BufferedReader br = new BufferedReader(new FileReader(new File("C:\\Users\\stefan\\Desktop\\AllDocumentKeys.txt")));
//        String line;
//        Map<Integer, Set<Integer>> usages = new HashMap<>();
//        Map<Integer, Integer> srs = new HashMap<>();
//        while ((line = br.readLine()) != null) {
//            if (line.contains("WP")) {
//                System.out.println(line);
//                ArtifactIdentifier ai = new ArtifactIdentifier(line, "IJamaArtifact");
//                Optional<IArtifact> opt = registry.get(ai, "irrelevant for this test");
//                JamaArtifact artifact = (JamaArtifact) opt.get();
//                usages.put(artifact.getId(), new HashSet<>(artifact.getDownstreamItemIds()));
//            } else if (line.contains("SRS")) {
//                System.out.println(line);
//                ArtifactIdentifier ai = new ArtifactIdentifier(line, "IJamaArtifact");
//                Optional<IArtifact> opt = registry.get(ai, "irrelevant for this test");
//                JamaArtifact artifact = (JamaArtifact) opt.get();
//                srs.put(artifact.getId(), 0);
//            }
//        }
//        for (Map.Entry<Integer, Integer> srsEntry : srs.entrySet()) {
//            for (Map.Entry<Integer, Set<Integer>> usageEntry : usages.entrySet()) {
//                if (usageEntry.getValue().contains(srsEntry.getKey())) {
//                    srsEntry.setValue(srsEntry.getValue()+1);
//                }
//            }
//        }
//        int usedOnce = 0;
//        int usedTwice = 0;
//        int usedNever = 0;
//        int usedMultiple = 0;
//        for (Map.Entry<Integer, Integer> srsEntry : srs.entrySet()) {
//            switch(srsEntry.getValue()) {
//                case 0:
//                    usedNever++;
//                    break;
//                case 1:
//                    usedOnce++;
//                    break;
//                case 2:
//                    usedTwice++;
//                    break;
//                default:
//                    usedMultiple++;
//            }
//        }
//        System.out.println(usedNever + " SRS are used never in a WP.");
//        System.out.println(usedOnce + " SRS are used in only one WP.");
//        System.out.println(usedTwice + " SRS are used in two different WPs.");
//        System.out.println(usedMultiple + " SRS are used in three or more WPs.");
//        System.out.println("done");
//    }

}
