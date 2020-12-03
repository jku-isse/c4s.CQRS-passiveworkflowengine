package impactassessment.jama;

import c4s.jamaconnector.OfflineHttpClientMock;
import c4s.jamaconnector.cache.CachedResourcePool;
import c4s.jamaconnector.cache.CouchDBJamaCache;
import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.json.SimpleJsonHandler;
import impactassessment.artifactconnector.ArtifactIdentifier;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.IArtifact;
import impactassessment.artifactconnector.jama.IJamaArtifact;
import impactassessment.artifactconnector.jama.JamaService;
import org.junit.Before;
import org.junit.Test;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.mockito.MockitoAnnotations;
import passiveprocessengine.instance.ArtifactWrapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FetchJamaArtifactsFromCouchDB {

    private JamaService jamaService;
    private ArtifactRegistry registry;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        JamaInstance jamaInstance = getJamaInstance();
        jamaService = new JamaService(jamaInstance, (set, correlationTuple) -> {/*do nothing*/});
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

    private CouchDbClient getCouchDbClient() {
        CouchDbProperties dbprops = new CouchDbProperties()
                .setDbName("jamaitems2")
                .setCreateDbIfNotExist(true)
                .setProtocol("http")
                .setHost("localhost")
                .setPort(5984)
                .setUsername("admin")
                .setPassword("password")
                .setMaxConnections(100)
                .setConnectionTimeout(0);
        return new CouchDbClient(dbprops);
    }

    private JamaInstance getJamaInstance() {
        CouchDbClient dbClient = getCouchDbClient();

        JamaConfig jamaConf = new JamaConfig();
        CouchDBJamaCache cache = new CouchDBJamaCache(dbClient);
//        jamaConf.setJson(new CachingJsonHandler(cache));
        jamaConf.setJson(new SimpleJsonHandler());
        jamaConf.setApiKey("SUPERSECRETKEY");
        String url = "http://localhost";
        jamaConf.setBaseUrl(url);
        jamaConf.setResourceTimeOut(Integer.MAX_VALUE);
        jamaConf.setOpenUrlBase(url);
        jamaConf.setUsername("OFFLINE");
        jamaConf.setPassword("OFFLINE");
        jamaConf.setResourceTimeOut(60);
        jamaConf.setHttpClient(new OfflineHttpClientMock());

        JamaInstance jamaInst = new JamaInstance(jamaConf, true);
        cache.setJamaInstance(jamaInst);
        jamaInst.setResourcePool(new CachedResourcePool(cache));
        return jamaInst;
    }
}
