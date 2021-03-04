package impactassessment.registry;

import impactassessment.kiesession.KieSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.kie.api.runtime.KieContainer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.persistance.json.DefinitionSerializer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class LocalRegisterService extends AbstractRegisterService {

    private DefinitionSerializer serializer = new DefinitionSerializer();
    private KieSessionFactory kieSessionFactory = new KieSessionFactory();

    public LocalRegisterService(WorkflowDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    public int registerAll() {
        int i = 0;
//        try {
//            ClassLoader cl = this.getClass().getClassLoader();
//            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
//            Resource[] folders = resolver.getResources("classpath:processdefinition/*") ;
//            for (Resource res : folders) {
//                Resource[] jsonResources = resolver.getResources(res.getURL()+"/*.json");
//                if (jsonResources.length != 1) continue;
//                WorkflowDefinition wfd = serializer.fromJson(asString(jsonResources[0]));
//
//                Resource[] drlResources = resolver.getResources(res.getURL()+"/*.drl");
//                if (drlResources.length < 1) continue;
//                List<File> files = new ArrayList<>();
//                for (Resource drl : drlResources) {
//                    files.add(drl.getFile());
//                }
//                KieContainer kieContainer = kieSessionFactory.getKieContainer(files);
//
//                registry.register(wfd.getId(), wfd, kieContainer);
//                i++;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // external resources (same directory as JAR)
        try {
            File[] directories = new File("./workflows/").listFiles(File::isDirectory);
            for (File dir : directories) {
                FileFilter fileFilter = new WildcardFileFilter("*.json");
                File[] jsonFiles = dir.listFiles(fileFilter);
                if (jsonFiles.length != 1) continue;
                byte[] encoded = Files.readAllBytes(jsonFiles[0].toPath());
                WorkflowDefinition wfd = serializer.fromJson(new String(encoded, Charset.defaultCharset()));

                FileFilter fileFilter2 = new WildcardFileFilter("*.drl");
                File[] drlFiles = dir.listFiles(fileFilter2);
                if (drlFiles.length < 1) continue;
                KieContainer kieContainer = kieSessionFactory.getKieContainer(Arrays.asList(drlFiles));

                registry.register(wfd.getId(), wfd, kieContainer);
                i++;
            }
        } catch (NullPointerException | IOException e) {
            log.warn("No external WFDs!");
        }

        log.info("LocalRegisterService registered {} process definitions from resources/processdefinition", i);
        return i;
    }

    private static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
