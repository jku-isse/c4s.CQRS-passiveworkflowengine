package impactassessment.registry;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRegisterService extends AbstractRegisterService {

	private static final JsonDefinitionSerializer serializer = new JsonDefinitionSerializer();
	
	public ProcessRegisterService(ProcessRegistry registry) {
		super(registry);
	}

	@Override
	public int registerAll() {
		int i = 0;
        // external resources (same directory as JAR)
        try {
            File[] directories = new File("./workflows/").listFiles(File::isDirectory);
            for (File dir : directories) {
                FileFilter fileFilter = new WildcardFileFilter("*.json");
                File[] jsonFiles = dir.listFiles(fileFilter);
                if (jsonFiles.length != 1) continue;
                byte[] encoded = Files.readAllBytes(jsonFiles[0].toPath());
                DTOs.Process procD = serializer.fromJson(new String(encoded, Charset.defaultCharset()));
                registry.storeProcessDefinitionIfNotExists(procD);
                i++;
            }
        } catch (NullPointerException | IOException e) {
            log.warn("No external process definitions found!");
        }

        log.info("ProcessRegisterService registered {} process definitions from resources/processdefinition", i);
        return i;
	}

}
