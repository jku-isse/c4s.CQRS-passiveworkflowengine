package impactassessment.registry;

import impactassessment.kiesession.KieSessionFactory;
import impactassessment.query.Replayer;
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

    public static String BASERULESDIR = "BASERULES-DONOTDELETE";
    
    public LocalRegisterService(WorkflowDefinitionRegistry registry, Replayer replayer) {
        super(registry, replayer);
    }

    public LocalRegisterService(WorkflowDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    public int registerAll() {
      
    	 List<File> baseRules = new ArrayList<File>();
        // load default rule set from jar file --> SOMEHOW THIS IS NOT FOUND WHEN RUNNING AS JAR
//        try {
//          ClassLoader cl = this.getClass().getClassLoader();
//          ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
//          Resource[] folders = resolver.getResources("classpath:defaultrules") ;
//          for (Resource res : folders) {
//              Resource[] drlResources = resolver.getResources(res.getURL()+"/*.drl");
//              if (drlResources.length < 1) continue;
//              for (Resource drl : drlResources) {
//            	  log.info("Loading defaultrule file: ",drl.getFilename());
//                  baseRules.add(drl.getFile());
//              }
//          }
//      } catch (IOException e) {
//          e.printStackTrace();
//      }

        
        try {
            File[] directories = new File("./workflows/").listFiles(File::isDirectory);
            for (File dir : directories) {
                if (dir.getName().equals(BASERULESDIR)) {
                    FileFilter fileFilter2 = new WildcardFileFilter("*.drl");
                    File[] drlFiles = dir.listFiles(fileFilter2);
                    if (drlFiles.length < 1) continue;
                    List<File> ruleFiles = new ArrayList<File>(Arrays.asList(drlFiles)); 
                    baseRules.addAll(ruleFiles);
                }
            }
        } catch (NullPointerException e) {
            log.warn("No external 'workflows' directory available");
        }
        baseRules.stream().forEach(file -> log.info("Loaded baserule: "+file.getName()));

        
        int i = 0;
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
                List<File> ruleFiles = new ArrayList<File>(Arrays.asList(drlFiles)); //asList() produces list that doesn't allow appending!!
                ruleFiles.addAll(baseRules);
                KieContainer kieContainer = kieSessionFactory.getKieContainer(ruleFiles);
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
