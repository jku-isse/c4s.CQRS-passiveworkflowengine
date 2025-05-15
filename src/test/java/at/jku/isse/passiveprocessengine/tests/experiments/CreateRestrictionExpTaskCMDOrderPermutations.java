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

class CreateRestrictionExpTaskCMDOrderPermutations {

	
	private static final String REPAIRONLY = "repaironly";
	private static final String NOREPAIR = "norepair";
	private static final String DEV = "dev";
	public static final String TASK_WARMUP = "_TaskWarmup";
	public static final String TASK_PRACISE = "PracticeTask"; 
	
// SET CORRECT PREFIX ---------------------------------------------------------------------------------------------------------------------------------------------
	private static final String AZURE_URL_PREFIX = "https://dev.azure.com/christophmayr-dorn0649/_workitems/edit/";
	private static final String AZURE_ARTIFACTTYPE = "azure_workitem";
	private static final String AZURE_IDTYPE = "azure_workitem";
	public static final String AZURE_INPUTPARAM = "CRs";
	
	@Test
	void createACLDataTableContent() {
		
// SET CORRECT RANGE OF PARTICIPANTS --------------------------------------------------------------------------------------------------------------------------------------		
		// for participants 1 to x (including)
		List<String> participantIds = IntStream.rangeClosed(26, 50)
                .mapToObj(x -> "P"+x)
                .toList(); 
		// for x participants above, 6 process inputs each
		// assumption that input are sorted by participant, i.e., the first n inputs (n = number of tasks=processes) belong to participant 1, etc.
// SET INPUT IDS -----------------------------------------------------------------------------------------------------------------------------------------------------		
		List<String> processInputIds = List.of(
				"P26_Project/11902", "P26_Project/11922", "P26_Project/11946", "P26_Project/11966", "P26_Project/11990", "P26_Project/12010",  
				"P27_Project/12044", "P27_Project/12064", "P27_Project/12088", "P27_Project/12108", "P27_Project/12132", "P27_Project/12152",  
				"P28_Project/12186", "P28_Project/12206", "P28_Project/12230", "P28_Project/12250", "P28_Project/12274", "P28_Project/12294",  
				"P29_Project/12328", "P29_Project/12348", "P29_Project/12372", "P29_Project/12392", "P29_Project/12416", "P29_Project/12436",  
				"P30_Project/12470", "P30_Project/12490", "P30_Project/12514", "P30_Project/12534", "P30_Project/12558", "P30_Project/12578",
				"P31_Project/12612", "P31_Project/12632", "P31_Project/12656", "P31_Project/12676", "P31_Project/12700", "P31_Project/12720",  
				"P32_Project/12754", "P32_Project/12774", "P32_Project/12798", "P32_Project/12818", "P32_Project/12842", "P32_Project/12862", 
				"P33_Project/12896", "P33_Project/12916", "P33_Project/12940", "P33_Project/12960", "P33_Project/12984", "P33_Project/13004",  
				"P34_Project/13038", "P34_Project/13058", "P34_Project/13082", "P34_Project/13102", "P34_Project/13126", "P34_Project/13146", 
				"P35_Project/13180", "P35_Project/13200", "P35_Project/13224", "P35_Project/13244", "P35_Project/13268", "P35_Project/13288", 
				"P36_Project/13322", "P36_Project/13342", "P36_Project/13366", "P36_Project/13386", "P36_Project/13410", "P36_Project/13430", 
				"P37_Project/13464", "P37_Project/13484", "P37_Project/13508", "P37_Project/13528", "P37_Project/13552", "P37_Project/13572",
				"P38_Project/13606", "P38_Project/13626", "P38_Project/13650", "P38_Project/13670", "P38_Project/13694", "P38_Project/13714", 
				"P39_Project/13748", "P39_Project/13768", "P39_Project/13792", "P39_Project/13812", "P39_Project/13836", "P39_Project/13856",  
				"P40_Project/13890", "P40_Project/13910", "P40_Project/13934", "P40_Project/13954", "P40_Project/13978", "P40_Project/13998", 
				"P41_Project/14032", "P41_Project/14052", "P41_Project/14076", "P41_Project/14096", "P41_Project/14120", "P41_Project/14140",  
				"P42_Project/14174", "P42_Project/14194", "P42_Project/14218", "P42_Project/14238", "P42_Project/14262", "P42_Project/14282",
				"P43_Project/14316", "P43_Project/14336", "P43_Project/14360", "P43_Project/14380", "P43_Project/14404", "P43_Project/14424", 
				"P44_Project/14458", "P44_Project/14478", "P44_Project/14502", "P44_Project/14522", "P44_Project/14546", "P44_Project/14566", 
				"P45_Project/14600", "P45_Project/14620", "P45_Project/14644", "P45_Project/14664", "P45_Project/14688", "P45_Project/14708",  
				"P46_Project/14742", "P46_Project/14762", "P46_Project/14786", "P46_Project/14806", "P46_Project/14830", "P46_Project/14850",
				"P47_Project/14884", "P47_Project/14904", "P47_Project/14928", "P47_Project/14948", "P47_Project/14972", "P47_Project/14992",  
				"P48_Project/15026", "P48_Project/15046", "P48_Project/15070", "P48_Project/15090", "P48_Project/15114", "P48_Project/15134", 
				"P49_Project/15168", "P49_Project/15188", "P49_Project/15212", "P49_Project/15232", "P49_Project/15256", "P49_Project/15276", 
				"P50_Project/15310", "P50_Project/15330", "P50_Project/15354", "P50_Project/15374", "P50_Project/15398", "P50_Project/15418"
				);

//  SET WARMUP IDS ---------------------------------------------------------------------------------------------------------------------------------------------		
		List<String> warmupInputs =List.of("P26_Project/12036","P27_Project/12178","P28_Project/12320","P29_Project/12462","P30_Project/12604",
				"P31_Project/12746","P32_Project/12888", "P33_Project/13030", "P34_Project/13172", "P35_Project/13314", 
				"P36_Project/13456",  "P37_Project/13598", "P38_Project/13740", "P39_Project/13882", "P40_Project/14024",
				"P41_Project/14166", "P42_Project/14308", "P43_Project/14450", "P44_Project/14592", "P45_Project/14734",
				 "P46_Project/14876", "P47_Project/15018", "P48_Project/15160", "P49_Project/15302", "P50_Project/15444" 
				);
		// and 6 processes types, representing the tasks		
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
		List<String> participantIds = IntStream.rangeClosed(26, 50)
                .mapToObj(x -> "P"+x)
                .toList(); 
// SET practise IDS ---------------------------------------------------------------------------------------------------------------------------------------------		
		List<String> practiseInputs //=new ArrayList<String>(List.of("T1/7697", "T2/7917","T3/8137", "T4/9677","T5/9897","T6/10117","T7/10337","T8/10557","T9/10777","T10/10997"));		
		= new ArrayList<String>(List.of("PracticeProject26/11252", "PracticeProject27/11278", "PracticeProject28/11304", "PracticeProject29/11330", "PracticeProject30/11356", "PracticeProject31/11382", "PracticeProject32/11408", "PracticeProject33/11434", "PracticeProject34/11460", "PracticeProject35/11486", 
				"PracticeProject36/11512", "PracticeProject37/11538", "PracticeProject38/11564", "PracticeProject39/11590", "PracticeProject40/11616", "PracticeProject41/11642", "PracticeProject42/11668", "PracticeProject43/11694", "PracticeProject44/11720", "PracticeProject45/11746", 
				"PracticeProject46/11772", "PracticeProject47/11798", "PracticeProject48/11824", "PracticeProject49/11850", "PracticeProject50/11876"));
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
