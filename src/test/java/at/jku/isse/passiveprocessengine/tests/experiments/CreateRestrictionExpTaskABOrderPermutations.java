package at.jku.isse.passiveprocessengine.tests.experiments;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequence;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequence.TaskInfo;
import at.jku.isse.passiveprocessengine.frontend.experiment.ProcessAccessControlProvider;

class CreateRestrictionExpTaskABOrderPermutations {

	
	private static final String REPAIRONLY = "repaironly";
	private static final String NOREPAIR = "norepair";
	private static final String DEV = "dev";
	public static final String TASK_WARMUP = "_TaskWarmup";
	public static final String TASK_PRACISE = "PracticeTask"; 
	

// TODO SET CORRECT PREFIX ---------------------------------------------------------------------------------------------------------------------------------------------
	//private static final String AZURE_URL_PREFIX = "https://dev.azure.com/christophmayr-dorn0649/_workitems/edit/";
	private static final String AZURE_URL_PREFIX = "https://dev.azure.com/anmolshiekh1/_workitems/edit/";
	private static final String AZURE_ARTIFACTTYPE = "azure_workitem";
	private static final String AZURE_IDTYPE = "azure_workitem";
	public static final String AZURE_INPUTPARAM = "CRs";
	
	@Test
	void createACLDataTableContent() {
		
// TODO SET CORRECT RANGE OF PARTICIPANTS --------------------------------------------------------------------------------------------------------------------------------------		
		// for participants 1 to x (including)
		List<String> participantIds = IntStream.rangeClosed(1, 25)
                .mapToObj(x -> "P"+x)
                .toList(); 
		// for x participants above, 6 process inputs each
		// assumption that input are sorted by participant, i.e., the first n inputs (n = number of tasks=processes) belong to participant 1, etc.

// TODO SET INPUT IDS -----------------------------------------------------------------------------------------------------------------------------------------------------		
		List<String> processInputIds = 
				List.of("P1_Project/1377", "P1_Project/1397", "P1_Project/1421", "P1_Project/1441", "P1_Project/1465", "P1_Project/1485", 
						"P2_Project/1519", "P2_Project/1539", "P2_Project/1563", "P2_Project/1583", "P2_Project/1607", "P2_Project/1627", 
						"P3_Project/1661", "P3_Project/1681", "P3_Project/1705", "P3_Project/1725", "P3_Project/1749", "P3_Project/1769",  			 
						"P4_Project/1803", "P4_Project/1823", "P4_Project/1847", "P4_Project/1867", "P4_Project/1891", "P4_Project/1911", 
						"P5_Project/1945", "P5_Project/1965", "P5_Project/1989", "P5_Project/2009", "P5_Project/2033", "P5_Project/2053", 
						"P6_Project/2087", "P6_Project/2107", "P6_Project/2131", "P6_Project/2151", "P6_Project/2175", "P6_Project/2195",
						"P7_Project/2229", "P7_Project/2249", "P7_Project/2273", "P7_Project/2293", "P7_Project/2317", "P7_Project/2337",   
						"P8_Project/2371", "P8_Project/2391", "P8_Project/2415", "P8_Project/2435", "P8_Project/2459", "P8_Project/2479",
						"P9_Project/2513", "P9_Project/2533", "P9_Project/2557", "P9_Project/2577", "P9_Project/2601", "P9_Project/2621",  
						"P10_Project/2655", "P10_Project/2675", "P10_Project/2699", "P10_Project/2719", "P10_Project/2743", "P10_Project/2763",
						"P11_Project/2797", "P11_Project/2817", "P11_Project/2841", "P11_Project/2861", "P11_Project/2885", "P11_Project/2905",
						"P12_Project/2939", "P12_Project/2959", "P12_Project/2983", "P12_Project/3003", "P12_Project/3027", "P12_Project/3047",
						"P13_Project/3081", "P13_Project/3101", "P13_Project/3125", "P13_Project/3145", "P13_Project/3169", "P13_Project/3189",
						"P14_Project/3223", "P14_Project/3243", "P14_Project/3267", "P14_Project/3287", "P14_Project/3311", "P14_Project/3331",
						"P15_Project/3365", "P15_Project/3385", "P15_Project/3409", "P15_Project/3429", "P15_Project/3453", "P15_Project/3473",
						"P16_Project/3507", "P16_Project/3527", "P16_Project/3551", "P16_Project/3571", "P16_Project/3595", "P16_Project/3615",
						"P17_Project/3649", "P17_Project/3669", "P17_Project/3693", "P17_Project/3713", "P17_Project/3737", "P17_Project/3757",
						"P18_Project/3791", "P18_Project/3811", "P18_Project/3835", "P18_Project/3855", "P18_Project/3879", "P18_Project/3899",
						"P19_Project/3933", "P19_Project/3953", "P19_Project/3977", "P19_Project/3997", "P19_Project/4021", "P19_Project/4041",
						"P20_Project/4075", "P20_Project/4095", "P20_Project/4119", "P20_Project/4139", "P20_Project/4163", "P20_Project/4183",
						"P21_Project/4217", "P21_Project/4237", "P21_Project/4261", "P21_Project/4281", "P21_Project/4305", "P21_Project/4325",
						"P22_Project/4359", "P22_Project/4379", "P22_Project/4403", "P22_Project/4423", "P22_Project/4447", "P22_Project/4467",
						"P23_Project/4501", "P23_Project/4521", "P23_Project/4545", "P23_Project/4565", "P23_Project/4589", "P23_Project/4609",
						"P24_Project/4643", "P24_Project/4663", "P24_Project/4687", "P24_Project/4707", "P24_Project/4731", "P24_Project/4751",
						"P25_Project/4785", "P25_Project/4805", "P25_Project/4829", "P25_Project/4849", "P25_Project/4873", "P25_Project/4893");
		/*List<String> processInputIds = List.of("T1/7485", "T1/7511", "T1/7531", "T1/7555", "T1/7581", "T1/7601", 
				"T2/7705", "T2/7731", "T2/7751", "T2/7775", "T2/7801", "T2/7821", 
				 "T3/7925", "T3/7951", "T3/7971", "T3/7995", "T3/8021", "T3/8041",  			 
				 "T4/9465", "T4/9491", "T4/9511", "T4/9535", "T4/9561", "T4/9581", 
				"T5/9685", "T5/9711", "T5/9731", "T5/9755", "T5/9781", "T5/9801", 
				 "T6/9905", "T6/9931", "T6/9951", "T6/9975", "T6/10001", "T6/10021",
				"T7/10125", "T7/10151", "T7/10171", "T7/10195", "T7/10221", "T7/10241",   
				"T8/10345", "T8/10371", "T8/10391", "T8/10415", "T8/10441", "T8/10461",  
				"T9/10565", "T9/10591", "T9/10611", "T9/10635", "T9/10661", "T9/10681",  
				"T10/10785", "T10/10811", "T10/10831", "T10/10855", "T10/10881", "T10/10901" );*/

// TODO SET WARMUP IDS ---------------------------------------------------------------------------------------------------------------------------------------------		
		//List<String> warmupInputs =List.of("T1/7697", "T2/7917","T3/8137", "T4/9677","T5/9897","T6/10117","T7/10337","T8/10557","T9/10777","T10/10997");
		List<String> warmupInputs =List.of("P1_Project/1511","P2_Project/1653","P3_Project/1795","P4_Project/1937",
				"P5_Project/2079","P6_Project/2221","P7_Project/2363", "P8_Project/2505","P9_Project/2647","P10_Project/2789",
				"P11_Project/2931","P12_Project/3073","P13_Project/3215","P14_Project/3357","P15_Project/3499","P16_Project/3641",
				"P17_Project/3783","P18_Project/3925", "P19_Project/4067", "P20_Project/4209","P21_Project/4351","P22_Project/4493",
				"P23_Project/4635","P24_Project/4777","P25_Project/4919");
		// and 6 processes types, representing the taskss		
		List<String> processTypeIds = List.of("Task1a", "Task1b", "Task2a", "Task2b", "Task3a", "Task3b");
		
		createPermutations(new ArrayList<String>(processInputIds), processTypeIds, participantIds,  new ArrayList<String>(warmupInputs), TASK_WARMUP);
	}

	public static void createPermutations(List<String> processInputIds, List<String> processTypeIds, List<String> participantIds, List<String> warmupInputs, String warmupTask) {
		// we need for every participant and task we need one dedicated input.
		assert(participantIds.size()==warmupInputs.size());
		
		assert((participantIds.size()*processTypeIds.size())==processInputIds.size());
		// we dont support more than 49 processTypes/experimentTasks
		assert(processTypeIds.size() < 50);
		
		initTaskOrderPermuations();
		// and permutate tasks 1-6 within groups of 2
		initRepairPermuations(); 
		
		Map<String,ExperimentSequence> participantData = createParticipantTaskOrderData(participantIds, processInputIds, processTypeIds, warmupInputs);
		storeExperimentSequenceToJsonFile(ProcessAccessControlProvider.FILENAME, participantData);		
	}
	
	@Test
	void createPractisePermissions() {
		
// SET CORRECT RANGE OF PARTICIPANTS --------------------------------------------------------------------------------------------------------------------------------------		
		// for participants 1 to x (excluding)
		List<String> participantIds = IntStream.range(51, 61)
                .mapToObj(x -> "P"+x)
                .toList(); 
// TODO SET practise IDS ---------------------------------------------------------------------------------------------------------------------------------------------		
		//List<String> practiseInputs =new ArrayList(List.of("T1/7697", "T2/7917","T3/8137", "T4/9677","T5/9897","T6/10117","T7/10337","T8/10557","T9/10777","T10/10997"));		
		/*List<String> practiseInputs =new ArrayList(List.of("PracticeProject1/558", "PracticeProject2/584", "PracticeProject3/610", "PracticeProject4/636", "PracticeProject5/662", "PracticeProject6/688",
				"PracticeProject7/714", "PracticeProject8/740", "PracticeProject9/766", "PracticeProject10/792", "PracticeProject11/818", "PracticeProject12/844", 
				"PracticeProject13/870", "PracticeProject14/896", "PracticeProject15/922", "PracticeProject16/948", "PracticeProject17/974", "PracticeProject18/1000", 
				"PracticeProject19/1026", "PracticeProject20/1052", "PracticeProject21/1078", "PracticeProject22/1104", "PracticeProject23/1130", "PracticeProject24/1156", 
				"PracticeProject25/1182"));		*/
		//Performance Test List
		List<String> practiseInputs =new ArrayList(List.of("P1/5016", "P2/5042", "P3/5068", "P4/5094", "P5/5120", "P6/5146", "P7/5172", "P8/5198", "P9/5224", "P10/5250"));
		assert(participantIds.size() == practiseInputs.size());		
		Map<String,ExperimentSequence> participantData = createPracticeTaskOrderData(participantIds, practiseInputs);
		storeExperimentSequenceToJsonFile(ProcessAccessControlProvider.FILENAME, participantData);	
	}

	
	public static List<List<Integer>> repairPerm = new LinkedList<>();
	public static List<List<Integer>> taskPerm = new LinkedList<>();
	
	public static void initRepairPermuations() {
		// no R0 for restriction experiement
		
		//(R1, R2, R0), (R1, R0, R2), (R2, R1, R0), (R2, R0, R1), (R0, R1, R2), and (R0, R2, R1)  	
		//(we use the same permutation for each task group, as task input order is separately determined and will increase task order diversity
		repairPerm.add(List.of(1,2,1,2,1,2));
		repairPerm.add(List.of(2,1,2,1,2,1));		
	}

	public static void initTaskOrderPermuations() {				
		// adapted for 6 tasks
		taskPerm.add(List.of(0,1, 2,3, 4,5));		
		taskPerm.add(List.of(0,1, 3,2, 4,5));
		taskPerm.add(List.of(0,1, 2,3, 5,4));		
		taskPerm.add(List.of(0,1, 3,2, 5,4));
		taskPerm.add(List.of(1,0, 2,3, 4,5));
		taskPerm.add(List.of(1,0, 3,2, 4,5));
		taskPerm.add(List.of(1,0, 2,3, 5,4));
		taskPerm.add(List.of(1,0, 3,2, 5,4));
									
//		taskPerm.add(List.of(2,0,1,5,3,4,8,6,7));
//		taskPerm.add(List.of(1,0,2,4,3,5,7,6,8));
//		taskPerm.add(List.of(2,1,0,5,4,3,8,7,6));		
//		taskPerm.add(List.of(0,1,2,3,4,5,6,7,8));
//		taskPerm.add(List.of(0,2,1,3,5,4,6,8,7));
//		taskPerm.add(List.of(1,2,0,4,5,3,7,8,6));
	}
	
	
	private static Map<String,ExperimentSequence> createParticipantTaskOrderData(List<String> participantIds, List<String> processInputIds, List<String> processTypeIds, List<String> processWarmupIds) {			
		Map<String,ExperimentSequence> sequences = new LinkedHashMap<>();
		AtomicInteger taskPermPointer = new AtomicInteger(1);
		AtomicInteger repairPermCounterSheet = new AtomicInteger(0);
		participantIds.stream().forEach(pId -> {
			ExperimentSequence seq = new ExperimentSequence(pId);
			String warmupId = processWarmupIds.remove(0);
			seq.getSequence().add(new TaskInfo(TASK_WARMUP,  AZURE_INPUTPARAM, warmupId, AZURE_ARTIFACTTYPE, AZURE_IDTYPE, permToRepairType(2), ProcessAccessControlProvider.SupportConfig.RESTRICTION.toString(), inputId2Url(warmupId)));
			Map<String, String> mapping = createTask2InputMap(processInputIds, processTypeIds);

			List<Integer> taskPermutation = taskPerm.get(taskPermPointer.getAndIncrement()%taskPerm.size());
			List<Integer> repairPermutation = repairPerm.get(repairPermCounterSheet.getAndIncrement()%repairPerm.size());
			for (int i = 0; i < processTypeIds.size(); i++) {
				seq.getSequence().add(permToTaskLine(i, taskPermutation, repairPermutation, mapping, processTypeIds));
			}
			sequences.put(pId, seq);
		});				
		
		ExperimentSequence seqDev = new ExperimentSequence(DEV);
		sequences.put(DEV, seqDev);
		seqDev.getSequence().add(new TaskInfo("*", null, null, null, null, null, ProcessAccessControlProvider.SupportConfig.RESTRICTION.toString(), null));
		
		ExperimentSequence seqNoRepair = new ExperimentSequence(NOREPAIR);
		sequences.put(NOREPAIR, seqNoRepair);
		seqNoRepair.getSequence().add(new TaskInfo("*", null, null, null, null, null, ProcessAccessControlProvider.SupportConfig.NONE.toString(), null));
		
		ExperimentSequence seqNoRestrictions = new ExperimentSequence(REPAIRONLY);
		sequences.put(REPAIRONLY, seqNoRestrictions);
		seqNoRestrictions.getSequence().add(new TaskInfo("*", null, null, null, null, null, ProcessAccessControlProvider.SupportConfig.REPAIR.toString(), null));
		
		return sequences;
	}
	
	private static Map<String,ExperimentSequence> createPracticeTaskOrderData(List<String> participantIds, List<String> processPractiseArtifactInputIds) {			
		Map<String,ExperimentSequence> sequences = new LinkedHashMap<>();
		
		participantIds.stream().forEach(pId -> {
			ExperimentSequence seq = new ExperimentSequence(pId);
			String pracId = processPractiseArtifactInputIds.remove(0);
			seq.getSequence().add(new TaskInfo(TASK_PRACISE,  AZURE_INPUTPARAM, pracId, AZURE_ARTIFACTTYPE, AZURE_IDTYPE, permToRepairType(2), ProcessAccessControlProvider.SupportConfig.RESTRICTION.toString(), inputId2Url(pracId)));
			sequences.put(pId, seq);
		});				
		
		ExperimentSequence seqDev = new ExperimentSequence(DEV);
		sequences.put(DEV, seqDev);
		seqDev.getSequence().add(new TaskInfo("*", null, null, null, null, null, ProcessAccessControlProvider.SupportConfig.RESTRICTION.toString(), null));
		
		ExperimentSequence seqNoRepair = new ExperimentSequence(NOREPAIR);
		sequences.put(NOREPAIR, seqNoRepair);
		seqNoRepair.getSequence().add(new TaskInfo("*", null, null, null, null, null, ProcessAccessControlProvider.SupportConfig.NONE.toString(), null));
		
		ExperimentSequence seqNoRestrictions = new ExperimentSequence(REPAIRONLY);
		sequences.put(REPAIRONLY, seqNoRestrictions);
		seqNoRestrictions.getSequence().add(new TaskInfo("*", null, null, null, null, null, ProcessAccessControlProvider.SupportConfig.REPAIR.toString(), null));
		
		return sequences;
	}
	
	private static String permToRepairType(int i) {
		if (i == 0)
			return "No repair support";
		if (i == 1)
			return "Repairs without restriction details";
		if (i == 2)
			return "Repairs with restriction details";
		else
			throw new IllegalArgumentException("RepairType must be 0, 1, or 2");
	}
	
	private static String inputId2Url(String input) {
		return AZURE_URL_PREFIX+input.split("/")[1];
	}
	
	private static ExperimentSequence.TaskInfo permToTaskLine(int index, List<Integer> taskPermutation, List<Integer> repairPermutation, Map<String, String> mapping, List<String> processTypeIds) {
	//	int processIndex = taskPermutation.indexOf(index);
		int processIndex = taskPermutation.get(index);
		String pType = processTypeIds.get(processIndex);
		int repairPerm = repairPermutation.get(processIndex);
		String inputId = mapping.get(pType);
		return new TaskInfo(pType,  AZURE_INPUTPARAM, inputId, AZURE_ARTIFACTTYPE, AZURE_IDTYPE, permToRepairType(repairPerm), ProcessAccessControlProvider.SupportConfig.values()[repairPerm].toString(),  inputId2Url(inputId));
	}
	
	private static Map<String, String> createTask2InputMap(List<String> processInputIds, List<String> processTypeIds) {
		Map<String, String> mapping = new HashMap<>();
		processTypeIds.stream().forEach(procId -> mapping.put(procId, processInputIds.remove(0)));
		return mapping;
	}
	
	private static void storeExperimentSequenceToJsonFile(String filename,  Map<String,ExperimentSequence> data) {
		Gson gson = new GsonBuilder()				 
				 .setPrettyPrinting()
				 .create();
		String json = gson.toJson(data);
		try {
			FileWriter file = new FileWriter(filename);
			file.write(json);
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
