package at.jku.isse.passiveprocessengine.frontend.experiment;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import at.jku.isse.designspace.artifactconnector.core.security.EmailToIdMapper;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.ArtifactIdentifier;
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
	private final ProcessRegistry processReg;
	private final ArtifactResolver resolver;
	private final EmailToIdMapper emailMapper;
	
	public ProcessAccessControlProvider(ProcessRegistry processReg, ArtifactResolver resolver, EmailToIdMapper emailMapper) {
		this.processReg = processReg;
		this.resolver = resolver;
		this.emailMapper = emailMapper;
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
								 || Objects.equals(SupportConfig.RESTRICTION.toString(), seq.getRepairSupportTypeId()) 
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
								 || Objects.equals(SupportConfig.RESTRICTION.toString(), seq.getRepairSupportTypeId()) 
								 || Objects.equals(SupportConfig.REPAIR.toString(), seq.getRepairSupportTypeId())
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
	
	/**
     *  Method that checks if current logged in OAuth user is allowed to instantiate process, if no OAuth user doAllowProcessInstantiation() is called
     */
	public boolean doAllowProcessInstantiationAuth(PPEInstance artifact) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if ( authentication.getPrincipal().getClass().equals(DefaultOAuth2User.class)) {	//OAuth authentication
			DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
			String userEmail = user.getAttribute("email");
			String userId = emailMapper.getIdForEmail(userEmail);
			return artifact.isOwner("*") || artifact.isOwner(userId); // authorizedUsers.contains(userId) || authorizedUsers.contains("*");	
		}
		// Otherwise static user. Call basic doAllowProcessInstantiation that relies on the ACL
		String authenticatedUserId = authentication != null ? authentication.getName() : null;
		return doAllowProcessInstantiation(artifact.getName(), authenticatedUserId);
	}
	
	public boolean doAllowProcessInstantiationAuth(ArtifactIdentifier procInputId) {
		try {
			PPEInstance artifact = resolver.get(procInputId);
			return doAllowProcessInstantiationAuth(artifact);
		} catch (ProcessException e) {
			e.printStackTrace();
			return false;
		}		
	}
	
	
	public String isAllowedAsNextProc(String procDefId, String authenticatedUserId) {
		if (procDefId == null || data.isEmpty() || authenticatedUserId == null)
			return procDefId;
		
		List<String> order = data.getOrDefault(authenticatedUserId, NULLSEQ).getSequence().stream()
				.map(seq -> seq.getProcessId())
				.collect(Collectors.toList());
		if (order.stream().allMatch(id -> id.equals("*"))) // any order allowed
			return procDefId;
		
		// check if that procDef has already been instantiated before, if so, then deny and search next 
		Optional<PPEInstance> procInst = findAnyProcessInstanceByDefinitionAndOwner(procDefId, authenticatedUserId);
		if (procInst.isPresent())
			return "Process already (previously) instantiated, cannot instantiate again";
		
		int pos = order.indexOf(procDefId);		
		// or if this is first one	
		if (pos == 0) return procDefId; // all ok, good to go
		if (pos > 0) {
			String prevProcDef = order.get(pos-1);
			Optional<PPEInstance> prevInst = findAnyProcessInstanceByDefinitionAndOwner(prevProcDef, authenticatedUserId);
			// if not yet instantiated, check if prior one has been closed
			if (prevInst.isPresent()) {
				PPEInstance prevP = prevInst.get();
				if (prevP.isMarkedAsDeleted())
					return procDefId; // all good to go
				else
					return "Previous process "+prevProcDef+" is not completed (and deleted) yet";
			} else {
				return "Previous process "+prevProcDef+" is not instantiated yet";
			}				 
		}
		return "You are not allowed to instantiate this process";
	}
	
	public Optional<PPEInstance> findAnyProcessInstanceByDefinitionAndOwner(String processDefinition, String owner) {
		//InstanceType procType =	ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, procReg.getProcessDefinition(processDefinition, true).get());
		// instances() does not return deleted instances!!
		return processReg.getExistingAndPriorInstances().stream()
			.filter(proc -> proc.getDefinition().getName().equals(processDefinition))
			.map(proc -> proc.getInstance())				
			.filter(instance -> instance.isOwner(owner))
			.findAny();		
	}
	
	public boolean isAuthorizedToView(PPEInstance el) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication.getPrincipal().getClass().equals(DefaultOAuth2User.class)) {
			DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
			String userEmail = user.getAttribute("email");			
			String userId = emailMapper.getIdForEmail(userEmail);
			return el.isOwner(userId) || el.isOwner("*");
		}
		return true;
	}
	
}
