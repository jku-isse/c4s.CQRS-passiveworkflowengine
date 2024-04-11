package at.jku.isse.passiveprocessengine.frontend.experiment;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.ReservedNames;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProcessAccessControlProvider {

	public enum SupportConfig {NONE, REPAIR, RESTRICTION};
	//public static String RESTRICTION_SELECTOR = "_RESTRICTION";
	//public static String REPAIR_SELECTOR = "_REPAIR";
	
	public static final String FILENAME = "./experiment/taskorder.json";
	private Map<String,ExperimentSequence> data = Collections.emptyMap();
	
	private static ExperimentSequence NULLSEQ = new ExperimentSequence(null);
	private ProcessRegistry processReg;
	
	public ProcessAccessControlProvider(ProcessRegistry processReg) {
		this.processReg = processReg;
		try {
			init();
		} catch (FileNotFoundException e) {						
			log.info("No experiment task order file found at: "+FILENAME);
		} catch (JsonIOException e1) {
			log.warn("Could not read task order file: "+e1.getMessage());			
		} catch (JsonSyntaxException e2) {
			log.warn("Could not read task order file: "+e2.getMessage());
		}
	}
	
	private void init() throws JsonIOException, JsonSyntaxException, FileNotFoundException {		
		Gson gson = new GsonBuilder()				 
				 .setPrettyPrinting()
				 .create();
		Type mapType = new TypeToken<Map<String,ExperimentSequence>>() {}.getType();		
		data = gson.fromJson(new FileReader(FILENAME) , mapType);
	}
	
	public ExperimentSequence getSequenceForParticipant(String pId) {
		return data.get(pId);
	}
	
	public boolean doShowRestrictions(ProcessInstance proc, String authenticatedUserId) {
		if (proc == null || data.isEmpty())
			return true;
		else {
			return data.getOrDefault(authenticatedUserId, NULLSEQ).getSequence().stream()						
				.anyMatch(seq -> seq.hasMatchingProcessAndSupport(proc.getDefinition().getName(), SupportConfig.RESTRICTION.toString())  
								 || Objects.equals("*", seq.getRepairSupportTypeId()) 
						);			
		}
	}
	
	
	
	public boolean doShowRepairs(ProcessInstance proc, String authenticatedUserId) {
		if (proc == null || data.isEmpty())
			return true;
		else {
			return data.getOrDefault(authenticatedUserId, NULLSEQ).getSequence().stream()						
				.anyMatch(seq -> seq.hasMatchingProcessAndSupport(proc.getDefinition().getName(), SupportConfig.REPAIR.toString())  
								 || seq.hasMatchingProcessAndSupport(proc.getDefinition().getName(), SupportConfig.RESTRICTION.toString()) 
								 || Objects.equals("*", seq.getRepairSupportTypeId()) 
								 || Objects.equals("+", seq.getRepairSupportTypeId())
						);			
		}				
	}
	
	public boolean doAllowProcessInstantiation(String procInputId, String authenticatedUserId) {
		if (procInputId == null || data.isEmpty())
			return true;
		else {
			return data.getOrDefault(authenticatedUserId, NULLSEQ).getSequence().stream()
					.anyMatch(seq -> Objects.equals(procInputId,seq.getInputId()) // if explicitly allowed
							|| Objects.equals("*",seq.getProcessId())				// or all processes allowed anyway
						);
		}				
	}
	
	public String isAllowedAsNextProc(String procDefId, String authenticatedUserId) {
		if (procDefId == null || data.isEmpty() || authenticatedUserId == null)
			return null;
		
		List<String> order = data.getOrDefault(authenticatedUserId, NULLSEQ).getSequence().stream()
				.map(seq -> seq.getProcessId())
				.collect(Collectors.toList());
		if (order.stream().allMatch(id -> id.equals("*"))) // any order allowed
			return null;
		
		// check if that procDef has already been instantiated before, if so, then deny and search next 
		Optional<Instance> procInst = findAnyProcessInstanceByDefinitionAndOwner(procDefId, authenticatedUserId);
		if (procInst.isPresent())
			return "Process already (previously) instantiated, cannot instantiate again";
		
		int pos = order.indexOf(procDefId);		
		// or if this is first one	
		if (pos == 0) return procDefId; // all ok, good to go
		if (pos > 0) {
			String prevProcDef = order.get(pos-1);
			Optional<Instance> prevInst = findAnyProcessInstanceByDefinitionAndOwner(prevProcDef, authenticatedUserId);
			// if not yet instantiated, check if prior one has been closed
			if (prevInst.isPresent()) {
				Instance prevP = prevInst.get();
				if (prevP.isDeleted)
					return procDefId; // all good to go
				else
					return "Previous process "+prevProcDef+" is not completed (and deleted) yet";
			} else {
				return "Previous process "+prevProcDef+" is not instantiated yet";
			}				 
		}
		return "You are not allowed to instantiate this process";
	}
	
	public Optional<Instance> findAnyProcessInstanceByDefinitionAndOwner(String processDefinition, String owner) {
		//InstanceType procType =	ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, procReg.getProcessDefinition(processDefinition, true).get());
		// instances() does not return deleted instances!!
		return processReg.getExistingAndPriorInstances().stream()
			.filter(proc -> proc.getDefinition().getName().equals(processDefinition))
			.map(proc -> proc.getInstance())				
			.filter(instance -> isOwner(owner, instance))
			.findAny();		
	}
	
	private boolean isOwner(String userName, Instance instance) {
		return instance.getPropertyAsSet(ReservedNames.OWNERSHIP_PROPERTY).get().stream()
			.map(strId -> Long.parseLong((String)strId))
			.map(id -> User.users.get((Long)id))
			.anyMatch(user -> ((User)user).name().equals(userName));
	}
}
