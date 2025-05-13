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

class CreateRestrictionExpTaskOrderPermutations {

	
	private static final String REPAIRONLY = "repaironly";
	private static final String NOREPAIR = "norepair";
	private static final String DEV = "dev";
	public static final String TASK_WARMUP = "_TaskWarmup";
	public static final String TASK_PRACISE = "XXXX"; //TODO: set ID for practise task!!!
	
// TODO SET CORRECT PREFIX ---------------------------------------------------------------------------------------------------------------------------------------------
	private static final String AZURE_URL_PREFIX = "https://dev.azure.com/christophmayr-dorn0649/_workitems/edit/";
	private static final String AZURE_ARTIFACTTYPE = "azure_workitem";
	private static final String AZURE_IDTYPE = "azure_workitem";
	public static final String AZURE_INPUTPARAM = "CRs";
	
	@Test
	void createACLDataTableContent() {
		
// TODO SET CORRECT RANGE OF PARTICIPANTS --------------------------------------------------------------------------------------------------------------------------------------		
		// for participants 1 to x (excluding)
		List<String> participantIds = IntStream.range(1, 11)
                .mapToObj(x -> "P"+x)
                .toList(); 
		// for x participants above, 6 process inputs each
		// assumption that input are sorted by participant, i.e., the first n inputs (n = number of tasks=processes) belong to participant 1, etc.
// TODO SET INPUT IDS -----------------------------------------------------------------------------------------------------------------------------------------------------		
		List<String> processInputIds = List.of("T1/7485", "T1/7511", "T1/7531", "T1/7555", "T1/7581", "T1/7601", 
				"T2/7705", "T2/7731", "T2/7751", "T2/7775", "T2/7801", "T2/7821", 
				 "T3/7925", "T3/7951", "T3/7971", "T3/7995", "T3/8021", "T3/8041",  			 
				 "T4/9465", "T4/9491", "T4/9511", "T4/9535", "T4/9561", "T4/9581", 
				"T5/9685", "T5/9711", "T5/9731", "T5/9755", "T5/9781", "T5/9801", 
				 "T6/9905", "T6/9931", "T6/9951", "T6/9975", "T6/10001", "T6/10021",
				"T7/10125", "T7/10151", "T7/10171", "T7/10195", "T7/10221", "T7/10241",   
				"T8/10345", "T8/10371", "T8/10391", "T8/10415", "T8/10441", "T8/10461",  
				"T9/10565", "T9/10591", "T9/10611", "T9/10635", "T9/10661", "T9/10681",  
				"T10/10785", "T10/10811", "T10/10831", "T10/10855", "T10/10881", "T10/10901" );

// TODO SET WARMUP IDS ---------------------------------------------------------------------------------------------------------------------------------------------		
		List<String> warmupInputs =List.of("T1/7697", "T2/7917","T3/8137", "T4/9677","T5/9897","T6/10117","T7/10337","T8/10557","T9/10777","T10/10997");
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
		
// TODO SET CORRECT RANGE OF PARTICIPANTS --------------------------------------------------------------------------------------------------------------------------------------		
		// for participants 1 to x (excluding)
		List<String> participantIds = IntStream.range(1, 11)
                .mapToObj(x -> "P"+x)
                .toList(); 
// TODO SET practise IDS ---------------------------------------------------------------------------------------------------------------------------------------------		
		List<String> practiseInputs =new ArrayList(List.of("T1/7697", "T2/7917","T3/8137", "T4/9677","T5/9897","T6/10117","T7/10337","T8/10557","T9/10777","T10/10997"));		
		
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
