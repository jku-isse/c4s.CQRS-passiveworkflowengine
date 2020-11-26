package impactassessment.kiesession;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Slf4j
public class KieSessionFactory {

    private static final String RULES_PATH = "rules/";

    private KieServices kieServices = KieServices.Factory.get();

    private void getKieRepository() {
        final KieRepository kieRepository = kieServices.getRepository();
        kieRepository.addKieModule(() -> kieRepository.getDefaultReleaseId());
    }

    public KieSession getKieSession(String... ruleFiles){
        getKieRepository();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        for (String ruleFile : ruleFiles) {
            kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH+ruleFile));
        }
        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();
        KieContainer kContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        KieSession kieSession = kContainer.newKieSession();
        KieSessionLogger.addRuleRuntimeEventListener(kieSession);
        KieSessionLogger.addAgendaEventListener(kieSession);
        return kieSession;
    }

    public KieContainer getKieContainer(List<File> ruleFiles) {
        getKieRepository();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        for (File ruleFile : ruleFiles) {
            kieFileSystem.write(ResourceFactory.newFileResource(ruleFile));
        }
        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    public KieContainer getKieContainerFromStrings(Collection<String> ruleFiles) {
        getKieRepository();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        int i = 0;
        for (String ruleFile : ruleFiles) {
            kieFileSystem.write("src/main/resources/rules/temp"+i+".drl", kieServices.getResources().newReaderResource( new StringReader(ruleFile) ));
            i++;
        }
        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

//    public KieSession getKieSession(Resource dt) {
//        KieFileSystem kieFileSystem = kieServices.newKieFileSystem().write(dt);
//        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();
//        KieRepository kieRepository = kieServices.getRepository();
//        ReleaseId krDefaultReleaseId = kieRepository.getDefaultReleaseId();
//        KieContainer kieContainer = kieServices.newKieContainer(krDefaultReleaseId);
//        KieSession kiesession = kieContainer.newKieSession();
//        return kiesession;
//    }

}
