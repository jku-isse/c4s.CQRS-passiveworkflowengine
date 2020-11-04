package impactassessment.registry;

import impactassessment.kiesession.KieSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.persistance.json.DefinitionSerializer;

import java.io.*;
import java.util.ArrayList;
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
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
            Resource[] folders = resolver.getResources("classpath:processdefinition/*") ;
            for (Resource res : folders) {
                Resource[] jsonResources = resolver.getResources(res.getURL()+"/*.json");
                if (jsonResources.length != 1) break;
                WorkflowDefinition wfd = serializer.fromJson(asString(jsonResources[0]));

                Resource[] drlResources = resolver.getResources(res.getURL()+"/*.drl");
                if (drlResources.length < 1) break;
                List<File> files = new ArrayList<>();
                for (Resource drl : drlResources) {
                    files.add(drl.getFile());
                }
                KieContainer kieContainer = kieSessionFactory.getKieContainer(files);

                registry.register(wfd.getId(), wfd, kieContainer);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
