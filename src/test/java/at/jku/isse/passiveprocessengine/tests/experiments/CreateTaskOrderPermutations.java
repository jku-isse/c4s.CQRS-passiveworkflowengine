package at.jku.isse.passiveprocessengine.tests.experiments;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
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
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequenceProvider;

class CreateTaskOrderPermutations {

	
	public static final String TASK_WARMUP = "_TaskWarmup";
	private static final String AZURE_URL_PREFIX = "https://dev.azure.com/christophmayr-dorn0649/_workitems/edit/";
	private static final String AZURE_ARTIFACTTYPE = "azure_workitem";
	private static final String AZURE_IDTYPE = "azure_workitem";
	public static final String AZURE_INPUTPARAM = "CRs";
	
	@Test
	void createACLDataTableContent() {

		// for participants 1 to x (excluding)
		List<String> participantIds = IntStream.range(1, 11)
                .mapToObj(x -> "P"+x)
                .collect(Collectors.toList()); 
		
		// for x participants above, 9 process inputs each
		// assumption that input are sorted by participant, i.e., the first n inputs (n = number of tasks=processes) belong to participant 1, etc.
		List<String> processInputIds = List.of("T1/7485", "T1/7511", "T1/7531", "T1/7555", "T1/7581", "T1/7601", "T1/7625", "T1/7651", "T1/7671", 
				"T2/7705", "T2/7731", "T2/7751", "T2/7775", "T2/7801", "T2/7821", "T2/7845", "T2/7871", "T2/7891", 
				 "T3/7925", "T3/7951", "T3/7971", "T3/7995", "T3/8021", "T3/8041", "T3/8065", "T3/8091", "T3/8111", 			 
				 "T4/9465", "T4/9491", "T4/9511", "T4/9535", "T4/9561", "T4/9581", "T4/9605", "T4/9631", "T4/9651",  
				"T5/9685", "T5/9711", "T5/9731", "T5/9755", "T5/9781", "T5/9801", "T5/9825", "T5/9851", "T5/9871", 
				 "T6/9905", "T6/9931", "T6/9951", "T6/9975", "T6/10001", "T6/10021", "T6/10045", "T6/10071", "T6/10091",  
				"T7/10125", "T7/10151", "T7/10171", "T7/10195", "T7/10221", "T7/10241", "T7/10265", "T7/10291", "T7/10311",  
				"T8/10345", "T8/10371", "T8/10391", "T8/10415", "T8/10441", "T8/10461", "T8/10485", "T8/10511", "T8/10531",  
				"T9/10565", "T9/10591", "T9/10611", "T9/10635", "T9/10661", "T9/10681", "T9/10705", "T9/10731", "T9/10751", 
				"T10/10785", "T10/10811", "T10/10831", "T10/10855", "T10/10881", "T10/10901", "T10/10925", "T10/10951", "T10/10971" );


		List<String> warmupInputs =List.of("T1/7697", "T2/7917","T3/8137", "T4/9677","T5/9897","T6/10117","T7/10337","T8/10557","T9/10777","T10/10997");
	
		// and 9 processes types, representing the taskss		
		List<String> processTypeIds = List.of("Task1a", "Task1b", "Task1c","Task2a", "Task2b", "Task2c","Task3a", "Task3b", "Task3c");
		
		System.out.println(createTableContent(new ArrayList<String>(processInputIds), processTypeIds, participantIds,  new ArrayList<String>(warmupInputs), TASK_WARMUP));
	}

	
//	@Test
//	void createTestACLDataTableContent() {
//
//		// for participants 1 to 30
//		List<String> participantIds = IntStream.range(1, 2)
//                .mapToObj(x -> "P"+x)
//                .collect(Collectors.toList()); 
//		
//		// for x participants above, 9 process inputs each
//		// assumption that input are sorted by participant, i.e., the first n inputs (n = number of tasks=processes) belong to participant 1, etc.
//		List<String> processInputIds = List.of("UserStudy1Prep/882", "UserStudy1Prep/883", "UserStudy1Prep/884", 
//				"UserStudy1Prep/885", "UserStudy1Prep/886", "UserStudy1Prep/887", 
//				"UserStudy1Prep/868", "UserStudy1Prep/888", "UserStudy1Prep/889");
//
//		// and 9 processes types, representing the taskss		
//		List<String> processTypeIds = List.of("Task1a", "Task1b", "Task1c","Task2a", "Task2b", "Task2c","Task3a", "Task3b", "Task3c");
//		String warmupTask = "_WarmupTest";
//		List<String> warmupInputs = List.of("UserStudy1Prep/8800");
//		
//
//		System.out.println(createTableContent(processInputIds, processTypeIds, participantIds, warmupInputs, warmupTask));
//		
//	}

	public static String createTableContent(List<String> processInputIds, List<String> processTypeIds, List<String> participantIds, List<String> warmupInputs, String warmupTask) {
		// we need for every participant and task we need one dedicated input.
		assert(participantIds.size()==warmupInputs.size());
		
		assert((participantIds.size()*processTypeIds.size())==processInputIds.size());
		// we dont support more than 49 processTypes/experimentTasks
		assert(processTypeIds.size() < 50);
		
		StringBuffer sb = new StringBuffer();
		/*
		The two types of permissions
		*/
		sb.append("INSERT INTO acl_class (id, class) VALUES\r\n"
				+ "(1, 'at.jku.isse.passiveprocessengine.frontend.security.persistence.ProcessProxy'),\r\n"
				+ "(2, 'at.jku.isse.passiveprocessengine.frontend.security.persistence.RestrictionProxy');");
		sb.append("\r\n \r\n");
		sb.append(createParticipantsTable(participantIds));
		sb.append("\r\n \r\n");
		initTaskOrderPermuations();
		sb.append(createProcessProxyTable(processInputIds, participantIds, processTypeIds, warmupTask, warmupInputs));						
		sb.append("\r\n \r\n");
		sb.append(createRestrictionProxyTable(processTypeIds, warmupTask));
		sb.append("\r\n \r\n");
		sb.append(createAclObjIdentityTable(processTypeIds, processInputIds, participantIds, warmupInputs, warmupTask));
		sb.append("\r\n \r\n");
		// and permutate tasks 1-9 within groups of three
		initRepairPermuations(); // switch to regular permutation for 9 tasks
		sb.append(createAclEntryTable(processTypeIds, processInputIds, participantIds, warmupInputs, warmupTask));
		
		Map<String,ExperimentSequence> participantData = createParticipantTaskOrderData(participantIds, processInputIds, processTypeIds, warmupInputs);
		storeExperimentSequenceToJsonFile(ExperimentSequenceProvider.FILENAME, participantData);
		return sb.toString();
	}
	
	public static List<List<Integer>> repairPerm = new LinkedList<>();
	public static List<List<Integer>> taskPerm = new LinkedList<>();
	
	public static void initRepairPermuations() {
		//(R1, R2, R0), (R1, R0, R2), (R2, R1, R0), (R2, R0, R1), (R0, R1, R2), and (R0, R2, R1)  	
		//(we use the same permutation for each task group, as task input order is separately determined and will increase task order diversity
		repairPerm.add(List.of(0,1,2,0,1,2,0,1,2));
		repairPerm.add(List.of(0,2,1,0,2,1,0,2,1));
		repairPerm.add(List.of(1,2,0,1,2,0,1,2,0));
		repairPerm.add(List.of(1,0,2,1,0,2,1,0,2));
		repairPerm.add(List.of(2,1,0,2,1,0,2,1,0));
		repairPerm.add(List.of(2,0,1,2,0,1,2,0,1));
	}

//	public static void initTestPermuations() {
//		//(R1, R2, R0), (R1, R0, R2), (R2, R1, R0), (R2, R0, R1), (R0, R1, R2), and (R0, R2, R1)  	
//		//(we use the same permutation for each task group, as task input order is separately determined and will increase task order diversity
//		perm.add(List.of(0,1,2));
//		perm.add(List.of(0,2,1));
//		perm.add(List.of(1,2,0));
//		perm.add(List.of(1,0,2));
//		perm.add(List.of(2,1,0));
//		perm.add(List.of(2,0,1));
//	}

	public static void initTaskOrderPermuations() {				
		// extended for 9 tasks
		taskPerm.add(List.of(2,0,1,5,3,4,8,6,7));
		taskPerm.add(List.of(1,0,2,4,3,5,7,6,8));
		taskPerm.add(List.of(2,1,0,5,4,3,8,7,6));		
		taskPerm.add(List.of(0,1,2,3,4,5,6,7,8));
		taskPerm.add(List.of(0,2,1,3,5,4,6,8,7));
		taskPerm.add(List.of(1,2,0,4,5,3,7,8,6));
	}
	
	/* 
	The participants 
	*/
	public static final String PARTICIPANTSHEADER = "INSERT INTO acl_sid (id, principal, sid) VALUES \r\n";	
	public static String createParticipantsTable(List<String> participantIds) {
		AtomicInteger counter = new AtomicInteger(1);
		return participantIds.stream()
				.map(id -> String.format("(%s, 1, '%s')", counter.getAndIncrement(), id))
				.collect(Collectors.joining(",\r\n", PARTICIPANTSHEADER, ",\r\n"
						+ "(980, 1, 'norepair'),\r\n"
						+ "(990, 1, 'repaironly'),\r\n"
						+ "(999, 1, 'dev'),\r\n"
						+ "(1000, 0, 'ROLE_EDITOR');"));
	}
	
	/* 
	every process instance, respectively the input artifact is listed here, one for each task X participant	
	*/
	public static final String PROCESSPROXYHEADER = "INSERT INTO processproxy(id,name) VALUES \r\n";			
	public static String createProcessProxyTable(List<String> processInputIds, List<String> participantIds, List<String> processTypeIds, String warmupTask, List<String> warmupInputs) {
		/* 	
		for every participant also the order of the tasks/processes within each group is determined 
		*/	
		String pythonOutTypes = processTypeIds.stream().map(id -> "'"+id+"'").collect(Collectors.joining(", ", "procTypes = (", ")\r\n"));
		System.out.println(pythonOutTypes);
		
		AtomicInteger counterPython = new AtomicInteger(1);
		AtomicInteger permPointerPython = new AtomicInteger(1);
		String pythonOut = participantIds.stream()
				.map(id -> String.format(" \"P%s\": %s ", counterPython.getAndIncrement(), createPythonSequence(permPointerPython, processTypeIds)))
				.collect(Collectors.joining("\r\n,", "taskorder = {", "}"));
		System.out.println(pythonOut+"\r\n");
		
		
		AtomicInteger rowCounter = new AtomicInteger(101);
		AtomicInteger permPointer = new AtomicInteger(1);
		Stream<String> s1= participantIds.stream()
				.map(id -> String.format("(%s, '%s')", rowCounter.getAndIncrement(), createSequence(permPointer, processTypeIds, warmupTask)));
				//.collect(Collectors.joining(",\r\n", PROCESSPROXYHEADER, ";"));
		
		AtomicInteger counter1 = new AtomicInteger(201);
		List<String> allInputs = new LinkedList<>(processInputIds);
		allInputs.addAll(warmupInputs);
		Stream<String> s2 = allInputs.stream()
				.map(id -> String.format("(%s, '%s')", counter1.getAndIncrement(), id));
				
		return Stream.concat(s1, s2).collect(Collectors.joining(",\r\n", PROCESSPROXYHEADER, ",\r\n(9999, '*');"));	
	}
	
	private static String createPythonSequence(AtomicInteger counterPerm, List<String> processTypeIds) {
		List<Integer> selPerm = taskPerm.get(counterPerm.getAndIncrement()%taskPerm.size());
		String pythonOut =  selPerm.stream()
				.map(index -> processTypeIds.get(index))			
				.collect(Collectors.joining("','","['","']"));		
		return pythonOut;
	}
	
	
	private static String createSequence(AtomicInteger permPointer, List<String> processTypeIds, String warmupProc) {
		// select permutation		
		List<Integer> selPerm = taskPerm.get(permPointer.getAndIncrement()%taskPerm.size());
		assert(selPerm.size()==processTypeIds.size());				
		String order = selPerm.stream()
			.map(index -> processTypeIds.get(index))			
			.collect(Collectors.joining("::"));
		return warmupProc+"::"+order;
	}
	
	/* 
	every process type is listed here twice, i.e., one for each task no Repair, with Repair, and with Repair and Restriction 
	(without repair and with restriction makes no sense and is not modelled) 
	*/
	public static final String RESTRICTIONPROXYHEADER = "INSERT INTO restrictionproxy(id,name) VALUES  \r\n";	
	public static String createRestrictionProxyTable(List<String> processTypeIds, String warmupTask) {
		List<String> allTypes = new LinkedList<>(processTypeIds);
		allTypes.add(warmupTask);
		AtomicInteger counter = new AtomicInteger(1);
		return allTypes.stream()
				.map(id -> String.format("(%s, '%s_REPAIR'),\r\n(%s, '%s_RESTRICTION')", counter.getAndIncrement(), id, counter.getAndIncrement(), id))
				.collect(Collectors.joining(",\r\n", RESTRICTIONPROXYHEADER, ",\r\n(9997, '+'),\r\n(9998, '*');"));
		// 																			9990 just repairs, 9999 repairs and restrictions
	}
		
	public static final String ACL_OBJ_IDENTITY_HEADER = "INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) VALUES \r\n";
	public static String createAclObjIdentityTable(List<String> processTypeIds, List<String> processInputIds, List<String> participantIds, List<String> warmupInputIds, String warmupTask) {
		/* 
		each process type (upper rows) and process instance resp input (lower rows) belongs to the role editor role, nothing else needed 
		*/
		StringBuffer content = new StringBuffer(ACL_OBJ_IDENTITY_HEADER);
		//entries for each restriction
		AtomicInteger counterIds = new AtomicInteger(1);
		List<String> allTypes = new LinkedList<>(processTypeIds);
		allTypes.add(warmupTask);
		content.append(allTypes.stream()
				.map(id -> String.format("(%s, 2, %s, NULL, 1000, 0), \r\n(%s, 2, %s, NULL, 1000, 0)", counterIds.get(), counterIds.getAndIncrement(), counterIds.get(), counterIds.getAndIncrement()))
				.collect(Collectors.joining(",\r\n")));
		content.append(",\r\n");
		
		// entries for each task order
		counterIds.set(101);
		AtomicInteger orderInput = new AtomicInteger(101);
		content.append(participantIds.stream()
				.map(id -> String.format("(%s, 1, %s, NULL, 1000, 0)", counterIds.getAndIncrement(), orderInput.getAndIncrement()))
				.collect(Collectors.joining(",\r\n")));
		content.append(",\r\n");

		// entries for each process input
		counterIds.set(201);
		AtomicInteger counterInput = new AtomicInteger(201);
		content.append(processInputIds.stream()
				.map(id -> String.format("(%s, 1, %s, NULL, 1000, 0)", counterIds.getAndIncrement(), counterInput.getAndIncrement()))
				.collect(Collectors.joining(",\r\n")));	
		content.append(",\r\n");
		// entries for each warmup input
		content.append(warmupInputIds.stream()
				.map(id -> String.format("(%s, 1, %s, NULL, 1000, 0)", counterIds.getAndIncrement(), counterInput.getAndIncrement()))
				.collect(Collectors.joining(",\r\n")));		
		content.append(",\r\n");
		
		content.append("(9997, 2, 9997, NULL, 1000, 0),"
				+ "\r\n(9998, 2, 9998, NULL, 1000, 0),\r\n"
					+ "(9999, 1, 9999, NULL, 1000, 0);");
		return content.toString();
	}
	

	public static final String ACL_ENTRY_HEADER = "INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid,           mask, granting, audit_success, audit_failure) VALUES \r\n";
	public static String createAclEntryTable(List<String> processTypeIds, List<String> processInputIds, List<String> participantIds, List<String> warmupInputIds, String warmupTask) {
		StringBuffer content = new StringBuffer(ACL_ENTRY_HEADER);
		/* 
		Every participant obtains read access to 'their' dedicated process instances as represented by the process input:
		for every participant (column 4) we store the access to their input data (e.g., 9x) via the table above, (column 2), i.e., ids above 100
		for these, ace order (column 3) can be 1 as there is only one permission for a single user per input artifact
		*/
		AtomicInteger counterRow1And3Ids = new AtomicInteger(1);
		AtomicInteger counterIntraParticipant = new AtomicInteger(0);
		AtomicInteger counterInputId = new AtomicInteger(201);
		List<String> allTypes = new LinkedList<>(processTypeIds);
		//allTypes.add(warmupTask);
		content.append(participantIds.stream()
				.flatMap(pId -> { 
					counterIntraParticipant.getAndIncrement();
					return allTypes.stream()
						.map(pType ->  String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterRow1And3Ids.get(), counterInputId.getAndIncrement(), counterRow1And3Ids.getAndIncrement(), counterIntraParticipant.get()));
					})
				.collect(Collectors.joining(",\r\n"))	
				);
		content.append(",\r\n");
		counterIntraParticipant.set(0); //reset participant counter to start again for warmup tasks
		content.append(participantIds.stream()
				.map(pId -> { 
					counterIntraParticipant.getAndIncrement();
					return String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterRow1And3Ids.get(), counterInputId.getAndIncrement(), counterRow1And3Ids.getAndIncrement(), counterIntraParticipant.get());
					})
				.collect(Collectors.joining(",\r\n"))	
				);
		
		assert((counterInputId.get()-1) == (processInputIds.size()+warmupInputIds.size()+200));
		//content.append(participants)
		
		content.append(",\r\n");

		/*
		 we provide every participant exactly a single read access to their task order
		 * */
		AtomicInteger counterOrder = new AtomicInteger(101);
		AtomicInteger counterP3 = new AtomicInteger(1);
		content.append(participantIds.stream()
				.map(pId ->  String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterRow1And3Ids.get(), counterOrder.getAndIncrement(), counterRow1And3Ids.getAndIncrement(), counterP3.getAndIncrement()) )
				.collect(Collectors.joining(",\r\n")));
		content.append(",\r\n");
		
		
		/* 
		We permutate for each participants the tasks in groups of three, 1 task without repairs R0 (no ACL entry), 1 task without restrictions R1 (one ACL entry), 1 task with both R2 (two ACL entries)
		The order of tasks are defined via the experiment protocol provided to the participants, as we cant control that via the ACL mechanism
		Possible 6 permutations within a group are:  (we use the same permutation for each task group, as task input order is separately determined and will increase task order diversity
		 (R1, R2, R0), (R1, R0, R2), (R2, R1, R0), (R2, R0, R1), (R0, R1, R2), and (R0, R2, R1)  	
		
		for every participant (column 4) we set for each process type when they are supposed to work with or without repairs and restrictions, i.e., ids 1-18 of the table above
		ace order (column 3) needs to count up from 1 to N (participants) per process type (1-9)   
		we only need READ writes, hence right part of matrix consists only of four 1s 
		*/
		AtomicInteger counterPythonPerm = new AtomicInteger(0);		
		String pythonOut = (participantIds.stream()
				.map(pId -> { 
					// select permutation
					List<Integer> selPerm = repairPerm.get(counterPythonPerm.getAndIncrement()%repairPerm.size());															
					return String.format(" \"%s\": %s ", pId, createPythonRepairPermutationEntry(selPerm, processTypeIds));
				})
				.collect(Collectors.joining(",\r\n", "repairPerm = {\r\n","}"))	
				);
		System.out.println("\r\n"+pythonOut);
		
		
		AtomicInteger counterP2 = new AtomicInteger(1);
		AtomicInteger counterPerm = new AtomicInteger(0);
		
		content.append(participantIds.stream()
				.flatMap(pId -> { 
					// select permutation
					List<Integer> selPerm = repairPerm.get(counterPerm.getAndIncrement()%repairPerm.size());										
					return createEntry(counterRow1And3Ids, counterP2.getAndIncrement(), new ArrayList<Integer>(selPerm), processTypeIds);
				})
				.collect(Collectors.joining(",\r\n"))	
				);
		content.append(",\r\n"
				+ "(9995, 9999, 1, 980,     1, 1, 1, 1),\r\n" // all processes, no repairs for user: norepair
				+ "(9996, 9999, 2, 990,     1, 1, 1, 1),\r\n" // only repairs for user :repaironly
				+ "(9997, 9997, 1, 990,     1, 1, 1, 1),\r\n" // all processes for user: repaironly
				+ "(9998, 9998, 1, 999,     1, 1, 1, 1),\r\n" //restrictions and repairs for user: dev
				+ "(9999, 9999, 3, 999,     1, 1, 1, 1);");    // all process for user : dev
		return content.toString();
	}
	
	private static Stream<String> createEntry(AtomicInteger counterIds, int participantId, List<Integer> permutation, List<String> processTypeIds) {
		assert(permutation.size()==processTypeIds.size());
		AtomicInteger counterTypes = new AtomicInteger(1);
		List<String> taskACL = permutation.stream()
			.flatMap(perm -> {
				if (perm == 0) {
					// no access, just increment to next type
					counterTypes.addAndGet(2);
					return Stream.empty();
				} else if (perm == 1) {
					// just repairs, no restrictions
					String rep = String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterIds.get(), counterTypes.getAndIncrement(), counterIds.getAndIncrement(), participantId);
					counterTypes.incrementAndGet(); // to skip the restriction permission
					return Stream.of(rep);
				} else {
					// repairs and restrictions
					return Stream.of(String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterIds.get(),  counterTypes.getAndIncrement(), counterIds.getAndIncrement(), participantId) ,
							String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterIds.get(), counterTypes.getAndIncrement(), counterIds.getAndIncrement(), participantId));
				}				
			}).collect(Collectors.toList());
		assert((counterTypes.get()+1)!=2*processTypeIds.size()); /*{
			System.out.println(counterTypes.get());
		}*/
		// add the warmup task permissions to see repairs and restrictions (warmup task is appended at the end of the processTypes List)
		taskACL.add(String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterIds.get(),  counterTypes.getAndIncrement(), counterIds.getAndIncrement(), participantId)); 
		taskACL.add(String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterIds.get(), counterTypes.getAndIncrement(), counterIds.getAndIncrement(), participantId));				
		return taskACL.stream();
	}
	
	private static String createPythonRepairPermutationEntry(List<Integer> permutation, List<String> processTypeIds) {
		AtomicInteger counterTypes = new AtomicInteger(0);		
		return permutation.stream()				
				.map(perm -> String.format(" \"%s\": \"T%s-R%s\" ", processTypeIds.get(counterTypes.get()), calcTaskGroup(counterTypes.getAndIncrement()) , perm))
				.collect(Collectors.joining(",", "{", "}"));
		
		
	}
	
	private static Map<String,ExperimentSequence> createParticipantTaskOrderData(List<String> participantIds, List<String> processInputIds, List<String> processTypeIds, List<String> processWarmupIds) {
		Map<String,ExperimentSequence> sequences = new HashMap<>();
		AtomicInteger taskPermPointer = new AtomicInteger(1);
		AtomicInteger repairPermCounterSheet = new AtomicInteger(0);
		participantIds.stream().forEach(pId -> {
			ExperimentSequence seq = new ExperimentSequence(pId);
			String warmupId = processWarmupIds.remove(0);
			seq.getSequence().add(new TaskInfo(TASK_WARMUP,  AZURE_INPUTPARAM, warmupId, AZURE_ARTIFACTTYPE, AZURE_IDTYPE, permToRepairType(2), inputId2Url(warmupId)));
			Map<String, String> mapping = createTask2InputMap(processInputIds, processTypeIds);

			List<Integer> taskPermutation = taskPerm.get(taskPermPointer.getAndIncrement()%taskPerm.size());
			List<Integer> repairPermutation = repairPerm.get(repairPermCounterSheet.getAndIncrement()%repairPerm.size());
			for (int i = 0; i < processTypeIds.size(); i++) {
				seq.getSequence().add(permToTaskLine(i, taskPermutation, repairPermutation, mapping, processTypeIds));
			}
			sequences.put(pId, seq);
		});
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
		return new TaskInfo(pType,  AZURE_INPUTPARAM, inputId, AZURE_ARTIFACTTYPE, AZURE_IDTYPE, permToRepairType(repairPerm), inputId2Url(inputId));
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
	
	
	private static int calcTaskGroup(int counter) {
		return Math.floorDiv(counter, 3)+1;		
	}
//	
//	@Test
//	void testFloor() {
//		System.out.println(Math.floorDiv(0, 3));
//		System.out.println(Math.floorDiv(1, 3));
//		System.out.println(Math.floorDiv(2, 3));
//		System.out.println(Math.floorDiv(3, 3));
//		System.out.println(Math.floorDiv(4, 3));
//		System.out.println(Math.floorDiv(5, 3));
//		System.out.println(Math.floorDiv(6, 3));
//		System.out.println(Math.floorDiv(7, 3));
//		System.out.println(Math.floorDiv(8, 3));
//	}
}
