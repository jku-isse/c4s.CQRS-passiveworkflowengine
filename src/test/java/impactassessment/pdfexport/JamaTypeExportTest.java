package impactassessment.pdfexport;

import com.google.inject.Injector;
import com.jamasoftware.services.restclient.exception.RestClientException;
import impactassessment.DevelopmentConfig;
import impactassessment.SpringConfig;
import impactassessment.artifactconnector.jama.IJamaService;
import impactassessment.artifactconnector.jama.JamaChangeSubscriber;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.pdfexport.PdfExporter;
import impactassessment.command.MockCommandGateway;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.fail;

import java.io.IOException;

public class JamaTypeExportTest {

//    @Test
//    void pdfExportJamaTypeCouchDB() {
//        SpringConfig conf = new SpringConfig();
//        IJamaService jamaService = conf.getJamaService(conf.getOnlineJamaInstance(conf.getJamaCache(conf.getCouchDbClient())), new JamaChangeSubscriber(new MockCommandGateway()));
//        PdfExporter exporter = new PdfExporter(jamaService);
//        try {
//            exporter.generate("IJamaArtifact");
//        } catch (IOException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }

    @Test
    void pdfExportJamaType() {
        Injector injector = DevelopmentConfig.getInjector();
        JamaService jamaService = injector.getInstance(JamaService.class);
        PdfExporter exporter = new PdfExporter(jamaService);
        try {
            exporter.generate("IJamaArtifact");
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

}
