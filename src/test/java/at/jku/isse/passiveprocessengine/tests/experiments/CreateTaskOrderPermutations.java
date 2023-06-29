package at.jku.isse.passiveprocessengine.tests.experiments;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class CreateTaskOrderPermutations {

	
	@Test
	void createACLDataTableContent() {

		// for participants 1 to 30
		List<String> participantIds = IntStream.range(1, 2)
                .mapToObj(x -> "P"+x)
                .collect(Collectors.toList()); 
		
		// for x participants above, 9 process inputs each
		// assumption that input are sorted by participant, i.e., the first n inputs (n = number of tasks=processes) belong to participant 1, etc.
		//List<String> processInputIds = List.of("UserStudy1Prep/882", "UserStudy1Prep/883", "UserStudy1Prep/884", 
		//		"UserStudy1Prep/885", "UserStudy1Prep/886", "UserStudy1Prep/887", 
		//		"UserStudy1Prep/868", "UserStudy1Prep/888", "UserStudy1Prep/889");
		List<String> processInputIds = List.of("P1-UserStudy2/988", "P1-UserStudy2/989", "P1-UserStudy2/990", "P1-UserStudy2/991", "P1-UserStudy2/992", "P1-UserStudy2/993", "P1-UserStudy2/994", "P1-UserStudy2/995", "P1-UserStudy2/996");
		
		// and 9 processes types, representing the taskss		
		List<String> processTypeIds = List.of("Task1a", "Task1b", "Task1c","Task2a", "Task2b", "Task2c","Task3a", "Task3b", "Task3c");
		
		

		System.out.println(createTableContent(processInputIds, processTypeIds, participantIds));
		
	}

	
	@Test
	void createTestACLDataTableContent() {

		// for participants 1 to 30
		List<String> participantIds = IntStream.range(1, 4)
                .mapToObj(x -> "P"+x)
                .collect(Collectors.toList()); 
		
		// for x participants above, 9 process inputs each
		// assumption that input are sorted by participant, i.e., the first n inputs (n = number of tasks=processes) belong to participant 1, etc.
		List<String> processInputIds = List.of("UserStudy1Prep/882", "UserStudy1Prep/883", "UserStudy1Prep/884", 
				"UserStudy1Prep/885", "UserStudy1Prep/886", "UserStudy1Prep/887", 
				"UserStudy1Prep/868", "UserStudy1Prep/888", "UserStudy1Prep/889");

		// and 9 processes types, representing the taskss		
		List<String> processTypeIds = List.of("Task1", "Task2", "Task3");
		
		

		System.out.println(createTableContent(processInputIds, processTypeIds, participantIds));
		
	}

	public static String createTableContent(List<String> processInputIds, List<String> processTypeIds, List<String> participantIds) {
		// we need for every participant and task we need one dedicated input.
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
		sb.append(createProcessProxyTable(processInputIds, participantIds, processTypeIds));						
		sb.append("\r\n \r\n");
		sb.append(createRestrictionProxyTable(processTypeIds));
		sb.append("\r\n \r\n");
		sb.append(createAclObjIdentityTable(processTypeIds, processInputIds, participantIds));
		sb.append("\r\n \r\n");
		// and permutate tasks 1-9 within groups of three
		initPermuations(); // switch to regular permutation for 9 tasks
		sb.append(createAclEntryTable(processTypeIds, processInputIds, participantIds));
		
		return sb.toString();
	}
	
	public static List<List<Integer>> perm = new LinkedList<>();
	public static List<List<Integer>> permTask = new LinkedList<>();
	
	public static void initPermuations() {
		//(R1, R2, R0), (R1, R0, R2), (R2, R1, R0), (R2, R0, R1), (R0, R1, R2), and (R0, R2, R1)  	
		//(we use the same permutation for each task group, as task input order is separately determined and will increase task order diversity
		perm.add(List.of(0,1,2,0,1,2,0,1,2));
		perm.add(List.of(0,2,1,0,2,1,0,2,1));
		perm.add(List.of(1,2,0,1,2,0,1,2,0));
		perm.add(List.of(1,0,2,1,0,2,1,0,2));
		perm.add(List.of(2,1,0,2,1,0,2,1,0));
		perm.add(List.of(2,0,1,2,0,1,2,0,1));
	}

	public static void initTestPermuations() {
		//(R1, R2, R0), (R1, R0, R2), (R2, R1, R0), (R2, R0, R1), (R0, R1, R2), and (R0, R2, R1)  	
		//(we use the same permutation for each task group, as task input order is separately determined and will increase task order diversity
		perm.add(List.of(0,1,2));
		perm.add(List.of(0,2,1));
		perm.add(List.of(1,2,0));
		perm.add(List.of(1,0,2));
		perm.add(List.of(2,1,0));
		perm.add(List.of(2,0,1));
	}

	public static void initTaskOrderPermuations() {				
		// extended for 9 tasks
		permTask.add(List.of(2,0,1,5,3,4,8,6,7));
		permTask.add(List.of(1,0,2,4,3,5,7,6,8));
		permTask.add(List.of(2,1,0,5,4,3,8,7,6));		
		permTask.add(List.of(0,1,2,3,4,5,6,7,8));
		permTask.add(List.of(0,2,1,3,5,4,6,8,7));
		permTask.add(List.of(1,2,0,4,5,3,7,8,6));

	}
	
	/* 
	The participants 
	*/
	public static final String PARTICIPANTSHEADER = "INSERT INTO acl_sid (id, principal, sid) VALUES \r\n";	
	public static String createParticipantsTable(List<String> participantIds) {
		AtomicInteger counter = new AtomicInteger(1);
		return participantIds.stream()
				.map(id -> String.format("(%s, 1, '%s')", counter.getAndIncrement(), id))
				.collect(Collectors.joining(",\r\n", PARTICIPANTSHEADER, ",\r\n(999, 1, 'dev'),\r\n(1000, 0, 'ROLE_EDITOR');"));
	}
	
	/* 
	every process instance, respectively the input artifact is listed here, one for each task X participant	
	*/
	public static final String PROCESSPROXYHEADER = "INSERT INTO processproxy(id,name) VALUES \r\n";			
	public static String createProcessProxyTable(List<String> processInputIds, List<String> participantIds, List<String> processTypeIds) {
		/* 	
		for every participant also the order of the tasks/processes within each group is determined 
		*/	
		String pythonOutTypes = processTypeIds.stream().map(id -> "'"+id+"'").collect(Collectors.joining(", ", "procTypes = (", ")\r\n"));
		System.out.println(pythonOutTypes);
		
		AtomicInteger counterPython = new AtomicInteger(1);
		AtomicInteger permCounterPython = new AtomicInteger(1);
		String pythonOut = participantIds.stream()
				.map(id -> String.format(" \"P%s\": %s ", counterPython.getAndIncrement(), createPythonSequence(permCounterPython, processTypeIds)))
				.collect(Collectors.joining("\r\n,", "taskorder = {", "}"));
		System.out.println(pythonOut+"\r\n");
		
		
		AtomicInteger counter = new AtomicInteger(101);
		AtomicInteger permCounter = new AtomicInteger(1);
		Stream<String> s1= participantIds.stream()
				.map(id -> String.format("(%s, '%s')", counter.getAndIncrement(), createSequence(permCounter, processTypeIds)));
				//.collect(Collectors.joining(",\r\n", PROCESSPROXYHEADER, ";"));
		
		AtomicInteger counter1 = new AtomicInteger(201);
		Stream<String> s2 = processInputIds.stream()
				.map(id -> String.format("(%s, '%s')", counter1.getAndIncrement(), id));
				
		return Stream.concat(s1, s2).collect(Collectors.joining(",\r\n", PROCESSPROXYHEADER, ",\r\n(9999, '*');"));		
	}
	
	private static String createPythonSequence(AtomicInteger counterPerm, List<String> processTypeIds) {
		List<Integer> selPerm = permTask.get(counterPerm.getAndIncrement()%permTask.size());
		String pythonOut =  selPerm.stream()
				.map(index -> processTypeIds.get(index))			
				.collect(Collectors.joining("','","['","']"));		
		return pythonOut;
	}
	
	
	private static String createSequence(AtomicInteger counterPerm, List<String> processTypeIds) {
		// select permutation		
		List<Integer> selPerm = permTask.get(counterPerm.getAndIncrement()%permTask.size());
		assert(selPerm.size()==processTypeIds.size());				
		return selPerm.stream()
			.map(index -> processTypeIds.get(index))			
			.collect(Collectors.joining("::"));
	}
	
	/* 
	every process type is listed here twice, i.e., one for each task no Repair, with Repair, and with Repair and Restriction 
	(without repair and with restriction makes no sense and is not modelled) 
	*/
	public static final String RESTRICTIONPROXYHEADER = "INSERT INTO restrictionproxy(id,name) VALUES  \r\n";	
	public static String createRestrictionProxyTable(List<String> processTypeIds) {
		AtomicInteger counter = new AtomicInteger(1);
		return processTypeIds.stream()
				.map(id -> String.format("(%s, '%s_REPAIR'),\r\n(%s, '%s_RESTRICTION')", counter.getAndIncrement(), id, counter.getAndIncrement(), id))
				.collect(Collectors.joining(",\r\n", RESTRICTIONPROXYHEADER, ",\r\n(9999, '*');"));
	}
		
	public static final String ACL_OBJ_IDENTITY_HEADER = "INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) VALUES \r\n";
	public static String createAclObjIdentityTable(List<String> processTypeIds, List<String> processInputIds, List<String> participantIds) {
		/* 
		each process type (upper rows) and process instance resp input (lower rows) belongs to the role editor role, nothing else needed 
		*/
		StringBuffer content = new StringBuffer(ACL_OBJ_IDENTITY_HEADER);
		//entries for each restriction
		AtomicInteger counterIds = new AtomicInteger(1);
		content.append(processTypeIds.stream()
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
		
		content.append("\r\n,(9998, 1, 9999, NULL, 1000, 0),\r\n"
					+ "(9999, 2, 9999, NULL, 1000, 0);");
		return content.toString();
	}
	

	public static final String ACL_ENTRY_HEADER = "INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid,           mask, granting, audit_success, audit_failure) VALUES \r\n";
	public static String createAclEntryTable(List<String> processTypeIds, List<String> processInputIds, List<String> participantIds) {
		StringBuffer content = new StringBuffer(ACL_ENTRY_HEADER);
		/* 
		Every participant obtains read access to 'their' dedicated process instances as represented by the process input:
		for every participant (column 4) we store the access to their input data (e.g., 9x) via the table above, (column 2), i.e., ids above 100
		for these, ace order (column 3) can be 1 as there is only one permission for a single user per input artifact
		*/
		AtomicInteger counterIds = new AtomicInteger(1);
		AtomicInteger counterP = new AtomicInteger(0);
		AtomicInteger counterInput = new AtomicInteger(201);
		content.append(participantIds.stream()
				.flatMap(pId -> { 
					counterP.getAndIncrement();
					return processTypeIds.stream()
						.map(pType ->  String.format("(%s, %s, 1, %s,     1, 1, 1, 1)", counterIds.getAndIncrement(), counterInput.getAndIncrement(), counterP.get()));
					})
				.collect(Collectors.joining(",\r\n"))	
				);
		assert((counterInput.get()-1) == (processInputIds.size()+200));
		content.append(",\r\n");

		/*
		 we provide every participant exactly a single read access to their task order
		 * */
		AtomicInteger counterOrder = new AtomicInteger(101);
		AtomicInteger counterP3 = new AtomicInteger(1);
		content.append(participantIds.stream()
				.map(pId ->  String.format("(%s, %s, %s, %s,     1, 1, 1, 1)", counterIds.get(), counterOrder.getAndIncrement(), counterIds.getAndIncrement(), counterP3.getAndIncrement()) )
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
					List<Integer> selPerm = perm.get(counterPythonPerm.getAndIncrement()%perm.size());															
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
					List<Integer> selPerm = perm.get(counterPerm.getAndIncrement()%perm.size());										
					return createEntry(counterIds, counterP2.getAndIncrement(), selPerm, processTypeIds);
				})
				.collect(Collectors.joining(",\r\n"))	
				);
		content.append(",\r\n"
				+ "(9998, 9998, 1, 999,     1, 1, 1, 1),\r\n"
				+ "(9999, 9999, 1, 999,     1, 1, 1, 1);");
		return content.toString();
	}
	
	private static Stream<String> createEntry(AtomicInteger counterIds, int participantId, List<Integer> permutation, List<String> processTypeIds) {
		assert(permutation.size()==processTypeIds.size());
		AtomicInteger counterTypes = new AtomicInteger(1);
		return permutation.stream()
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
			});
		
	}
	
	private static String createPythonRepairPermutationEntry(List<Integer> permutation, List<String> processTypeIds) {
		AtomicInteger counterTypes = new AtomicInteger(0);		
		return permutation.stream()				
				.map(perm -> String.format(" \"%s\": \"T%s-R%s\" ", processTypeIds.get(counterTypes.get()), calcTaskGroup(counterTypes.getAndIncrement()) , perm))
				.collect(Collectors.joining(",", "{", "}"));
		
		
	}
	
	private static int calcTaskGroup(int counter) {
		return Math.floorDiv(counter, 3)+1;		
	}
	
	@Test
	void testFloor() {
		System.out.println(Math.floorDiv(0, 3));
		System.out.println(Math.floorDiv(1, 3));
		System.out.println(Math.floorDiv(2, 3));
		System.out.println(Math.floorDiv(3, 3));
		System.out.println(Math.floorDiv(4, 3));
		System.out.println(Math.floorDiv(5, 3));
		System.out.println(Math.floorDiv(6, 3));
		System.out.println(Math.floorDiv(7, 3));
		System.out.println(Math.floorDiv(8, 3));
	}
}
