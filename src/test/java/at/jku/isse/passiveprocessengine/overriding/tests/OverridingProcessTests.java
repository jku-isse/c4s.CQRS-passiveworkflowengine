package at.jku.isse.passiveprocessengine.overriding.tests;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.PPE3Webfrontend;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry.ProcessDeployResult;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.registry.TriggeredProcessLoader;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import c4s.processdefinition.blockly2java.Transformer;
import c4s.processdefinition.blockly2java.Xml2Java;
import https.developers_google_com.blockly.xml.Xml;
import lombok.extern.slf4j.Slf4j;

@ExtendWith(SpringExtension.class)

@Slf4j
@SpringBootTest(classes = PPE3Webfrontend.class)
public class OverridingProcessTests {

	@Autowired
	ProcessRegistry procReg;

	@Autowired
	ArtifactResolver artRes;

	TriggeredProcessLoader tpl=new TriggeredProcessLoader(procReg,artRes);
	
	public static final JsonDefinitionSerializer serializer = new JsonDefinitionSerializer();

	@Test
	void OV0_testWorkItemWithCondition_0() throws ProcessException {
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess0.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("QA1 'IsOverrideable' property might require to be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV22_testWorkItemWithCondition_0V1() throws ProcessException {
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess22.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("QA1 'IsOverrideable' property might require to be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV_23testWorkItemWithCondition_0V2() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess23.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("QA1 'IsOverrideable' property might require to be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV15_testWorkItemWithCondition_1() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess15.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("QA1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV5_testWorkItemWithCondition_2() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess5.xml");
		List<String> expected_warnings=new LinkedList<>();
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV7_testWorkItemWithCondition_3() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess7.xml");
		List<String> expected_warnings=new LinkedList<>();
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV18_testWorkItemWithCondition_4() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess18.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("POSTCONDITION1 'IsOverrideable' property might require to be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV20_testWorkItemWithCondition_5() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess20.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("POSTCONDITION1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV19_testWorkItemWithCondition_6() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess19.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("POSTCONDITION1 'IsOverrideable' property might require to be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}


	@Test
	void OV8_testWorkItemWithCondition_7V0() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess8.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const3 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV24_testWorkItemWithCondition_7V1() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess24.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const3 'IsOverrideable' property might require to be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV9_testWorkItemWithCondition_8() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess9.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("QA1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV10_testWorkItemWithCondition_9() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess10.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("QA1 'IsOverrideable' property must be ENABLED.");
		expected_warnings.add("POSTCONDITION1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV21_testWorkItemWithCondition_10() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess21.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("QA1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void Ov4_testSizeWithPath_0() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess4.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const1 'IsOverrideable' property might require to be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : "Warning Lists are Not Equivalent";
	}


	@Test
	void OV2_testOVConditionInBetween_0() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess2.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("QA1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV13_testSizeWithPath_4() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess13.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV16_testSizeWithPath_2() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess16.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}

	@Test
	void OV3_testSizeWithPath_6() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess3.xml");
		List<String> expected_warnings=new LinkedList<>();
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}
	
	@Test
	void OV6_testSizeWithPath_1() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess6.xml");
		List<String> expected_warnings=new LinkedList<>();
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}
	
	@Test
	void OV1_testSizeWithPath_5() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess1.xml");
		List<String> expected_warnings=new LinkedList<>();
		assert listsEqual(generated_warnings, expected_warnings) : "Warning Lists are Not Equivalent";
	}

	@Test
	void OV17_testSizeWithPath_3() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess17.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const1 'IsOverrideable' property might require to be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}
	
	@Test
	void OV11_testSizeWithPath_7() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess11.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}
	
	@Test
	void OV12_testSizeWithPath_8() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess12.xml");
		List<String> expected_warnings=new LinkedList<>();
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}
	
	@Test
	void OV14_testSizeWithPath_9() throws ProcessException {
		
		List<ProcessDefinitionError> generated_warnings=registerFromBlocklyXML("OverrideProcess14.xml");
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}
	
	/*@Test
	void OV25_IndustryConstraint1And2() throws ProcessException {
		registerFromBlocklyXML("OverrideProcess25.xml");
		List<ProcessDefinitionError> generated_warnings=procReg.getOverride_warnings();
		List<String> expected_warnings=new LinkedList<>();
		expected_warnings.add("Const1 'IsOverrideable' property must be ENABLED.");
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}*/
    /*@Test
	void SRA_testIndustryConstraint() throws ProcessException {
		registerJson("SystemRequirementsAnalysis(WIP).json");
		List<ProcessDefinitionError> generated_warnings=procReg.getOverride_warnings();
		List<String> expected_warnings=new LinkedList<>();
		assert listsEqual(generated_warnings, expected_warnings) : ("Warning Lists are Not Equivalent. \n"+this.getList(generated_warnings));
	}*/

	

	public int registerJson(String fileName) {
		int i = 0;
        // external resources (same directory as JAR)
        try {
            File directory = new File("./processdefinitions//overridingProcesses/"); // no longer files in separate directories, all in a single one now
            FileFilter fileFilter = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.toString().endsWith(fileName);
				}}; //new WildcardFileFilter("*.json");
            File[] jsonFiles = directory.listFiles(fileFilter);
            for (File jsonFile : jsonFiles) {
                byte[] encoded = Files.readAllBytes(jsonFile.toPath());
                DTOs.Process procD = serializer.fromJson(new String(encoded, Charset.defaultCharset()));
                if (procD != null) {
                	try {
                		SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>>  result = procReg.storeProcessDefinition(procD, false);
						if (result != null && !result.getValue().isEmpty()) {
							System.out.println("Error loading process definition from file system: "+result.getKey().getName()+"\r\n"+result.getValue());
						} else if (result == null) {
							System.out.println("Loading of process definition "+procD.getCode()+" defered to when workspace is available");
						}
                		i++;
					} catch (NullPointerException e) {
						e.printStackTrace();
						//log.error(e.getMessage());
					}
                	
                }
            }
        } catch (NullPointerException | IOException e) {
        	System.out.println("No external process definitions found in ./processdefinitions/ ");
        }

        System.out.println("registered {} process definitions from folder ./processdefinitions/" + i);
        return i;
	}
	
/*	protected List<ProcessDefinitionError> registerFromBlocklyXML(String fileName) {
		int i = 0;
		// external resources (same directory as JAR)
		try {
			File directory = new File("./processdefinitions//overridingProcesses/"); // no longer files in separate directories, all in a single one now
			FileFilter fileFilter = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					//return pathname.toString().endsWith(".xml");
					return pathname.toString().endsWith(fileName);
				}}; //new WildcardFileFilter("*.json");
				File[] xmlFiles = directory.listFiles(fileFilter);
				for (File xmlFile : xmlFiles) {                
					byte[] encoded = Files.readAllBytes(xmlFile.toPath());
					String xml = new String(encoded, Charset.defaultCharset());
					String json = processBlocklyXML(xml);
					DTOs.Process procD = tpl.serializer.fromJson(json);
					if (procD != null) {
						try {
							if (!procReg.getProcessDefinition(procD.getCode(), true).isPresent()) {
								// we wont override correct existing json based definition
								SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>>  result = procReg.storeProcessDefinition(procD, false);
								
								if (result != null && !result.getValue().isEmpty()) {
									System.out.println("Error loading process definition from file system: "+result.getKey().getName()+"\r\n"+result.getValue());
								} else if (result == null) {
									System.out.println("Loading of process definition "+procD.getCode()+" defered to when workspace is available");
								}
								return result;
								i++;
							}
						} catch (NullPointerException e) {
							e.printStackTrace();
							//log.error(e.getMessage());
						}

					}
				}
		} catch (Exception e) {
			System.out.println("No external Blockly XML-based process definitions found in ./processdefinitions/ ");
		}
		System.out.println("registered additional {} XML-based process definitions from folder ./processdefinitions/"+ i);     
	}*/

	/*public String processBlocklyXML(String xml) throws JAXBException { //Map<String, Object>
		Xml2Java x2j = new Xml2Java();
		Transformer t = new Transformer();
		Optional<Xml> optRoot = x2j.parse(xml);
		List<String> resp = new LinkedList<>();
		if (optRoot.isPresent()) {
			t.toProcessDefinition(optRoot.get()).stream().forEach(proc -> {
				JsonDefinitionSerializer dser = new JsonDefinitionSerializer();
				String defString = dser.toJson(proc);
				resp.add(defString);
			});
		}
		return resp.size() > 0 ? resp.get(0) : "";		
	}
*/
	
	protected List<ProcessDefinitionError> registerFromBlocklyXML(String fileName) {
		int i = 0;
		// external resources (same directory as JAR)
		try {
			File directory = new File("./processdefinitions/"); // no longer files in separate directories, all in a single one now
			FileFilter fileFilter = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.toString().endsWith(fileName);
				}}; //new WildcardFileFilter("*.json");
				File[] xmlFiles = directory.listFiles(fileFilter);
				for (File xmlFile : xmlFiles) {                
					byte[] encoded = Files.readAllBytes(xmlFile.toPath());
					String xml = new String(encoded, Charset.defaultCharset());
					String json = processBlocklyXML(xml);
					DTOs.Process procD = serializer.fromJson(json);
					if (procD != null) {
						try {
							if (!procReg.getProcessDefinition(procD.getCode(), true).isPresent()) {
								// we wont override correct existing json based definition
								ProcessDeployResult result = procReg.createProcessDefinitionIfNotExisting(procD);
								if (result != null && !result.getDefinitionErrors().isEmpty()) {
									log.warn("Error loading process definition from file system: "+procD.getCode()+"\r\n"+result.getDefinitionErrors().toString());
								} else if (result == null) {
									log.info("Loading of process definition "+procD.getCode()+" defered to when workspace is available");
								}
								i++;
								return result.getDefinitionErrors();
							}
						} catch (NullPointerException e) {
							e.printStackTrace();
							log.error(e.getMessage());
						}

					}
				}
		} catch (Exception e) {
			log.info("No external Blockly XML-based process definitions found in ./processdefinitions/ ");
		}
		log.info("registered additional {} XML-based process definitions from folder ./processdefinitions/", i);   
		return null;
	}
	
	public String processBlocklyXML(String xml) throws JAXBException { //Map<String, Object>
		Xml2Java x2j = new Xml2Java();
		Transformer t = new Transformer();
		Optional<Xml> optRoot = x2j.parse(xml);
		List<String> resp = new LinkedList<>();
		if (optRoot.isPresent()) {
			t.toProcessDefinition(optRoot.get()).stream().forEach(proc -> {
				JsonDefinitionSerializer dser = new JsonDefinitionSerializer();
				String defString = dser.toJson(proc);
				resp.add(defString);
			});
		}
		return resp.size() > 0 ? resp.get(0) : "";		
	}
	
	protected String getList(List<ProcessDefinitionError> generated_warnings)
	{
		String ret="";
		for(int i=0;i<generated_warnings.size();i++)
			ret=ret+generated_warnings.get(i).getErrorMsg()+"\n";
		return ret;
	}
	
	private static boolean listsEqual(List<ProcessDefinitionError> generatedList, List<String> expoectedList) {
		if (generatedList.size() != expoectedList.size()) {
			return false;
		}
		for (int i = 0; i < generatedList.size(); i++) {
			if (!generatedList.get(i).getErrorMsg().equals(expoectedList.get(i))) {
				return false;
			}
		}
		return true;
	}

}
