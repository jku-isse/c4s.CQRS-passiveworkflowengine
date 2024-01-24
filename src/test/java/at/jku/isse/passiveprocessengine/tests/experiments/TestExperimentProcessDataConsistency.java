package at.jku.isse.passiveprocessengine.tests.experiments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequence;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequence.TaskInfo;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityConfig;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequenceProvider;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.RepairAnalyzer;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.NonNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestExperimentProcessDataConsistency {
	
	@Autowired
	RequestDelegate reqDelegate;

	@Autowired
	ProcessRegistry procReg;
	
	@Autowired
	ArtifactResolver artRes;
	
	@Autowired
	RepairAnalyzer repairanalyzer;
	
	@Autowired
	ProcessQAStatsMonitor qastats;
	
	@Autowired 
	UsageMonitor usageMonitor;
	
	@Autowired
	ExperimentSequenceProvider expSeqProvider;
	
	@Autowired
	SecurityService securityService;
	
	@Autowired
	AuthenticationManager authManager;
	
	private enum PROCESSTYPEIDS {_TaskWarmup, Task1a, Task1b, Task1c, Task2a, Task2b, Task2c, Task3a, Task3b, Task3c};
	
	private enum OUTPUTS { REQs, TCs, Bugs };
	
	private Map<String,String> credentials = SecurityConfig.getExperimentUserCredentials();
	
	List<String> participantIds = List.of("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10");
	
	@BeforeAll
	static void setUp() throws Exception {
	}
	

	@Test
	void checkIfParticipantsHaveAccessWithinPPE() {
		List<String> allResults = participantIds.stream()
				.map(participantId -> {
					ExperimentSequence expSeq = expSeqProvider.getSequenceForParticipant(participantId);
					List<Boolean> result = expSeq.getSequence().stream().map(info -> {
						try {
							setUserAuthentication(participantId);
							return instantiateAndClose(info, participantId);
						} catch (ProcessException e) {
							e.printStackTrace();
							return false;
						}
					})
							.filter(Objects::nonNull)
							.collect(Collectors.toList());
					if (result.stream().allMatch(singleRes -> singleRes==true)) {
						return  "All processes accessible for participant: "+participantId;
					} else {
						return "Processes not completely accessible for participant: "+participantId+ " "+result.toString();
					}
				})
				.collect(Collectors.toList());
		System.out.println(allResults);
	}
	

	@Test
	void checkIfExperimentDataIsConsistent() throws ProcessException {
		// for each user
		List<String> allResults = participantIds.stream()
				.map(participantId -> {
			ExperimentSequence expSeq = expSeqProvider.getSequenceForParticipant(participantId);
			List<Boolean> result = expSeq.getSequence().stream().map(info -> {
				try {
					return instantiate(info);
				} catch (ProcessException e) {
					e.printStackTrace();
					return null;
				}
			})
			.filter(Objects::nonNull)
			.map(pi -> {
				return checkDataConsistency(pi);
			})
			.collect(Collectors.toList());
			if (result.stream().allMatch(singleRes -> singleRes==true)) {
				return "All processes ok for participant: "+participantId;
			} else {
				return "Processes with error for participant: "+participantId+ " "+result.toString();
			}
		})
		.collect(Collectors.toList());
		System.out.println(allResults);		
	}
	
	private boolean instantiateAndClose(TaskInfo entry, String participantId) throws ProcessException {
		String nextAllowedProc = reqDelegate.isAllowedAsNextProc(entry.getProcessId(), participantId);
		boolean allowInstantiation = reqDelegate.doAllowProcessInstantiation(entry.getInputId()) && nextAllowedProc.equalsIgnoreCase(entry.getProcessId());
		if (!allowInstantiation) {
			System.out.println(String.format("User %s not allowed to init process %s ", participantId, entry.getProcessId()));
			return false;
		} else {
			ProcessInstance procInst = instantiate(entry);
			procInst.getInstance().addOwner(new User(participantId));
			// now delete process again
			reqDelegate.deleteProcessInstance(procInst.getName());
			return true;
		}
	}
	
	private void setUserAuthentication(String participantId) {
		UsernamePasswordAuthenticationToken authReq
	      = new UsernamePasswordAuthenticationToken(participantId, credentials.get(participantId));
	    Authentication auth = authManager.authenticate(authReq);
	    SecurityContextHolder.getContext().setAuthentication(auth);
	}
	
	private ProcessInstance instantiate(TaskInfo entry) throws ProcessException{
		String id = entry.getInputId()+entry.getProcessId();
		Map<String, ArtifactIdentifier> inputs = new HashMap<>();
		inputs.put(entry.getInputParam(), new ArtifactIdentifier(entry.getInputId(), entry.getArtifactType(), entry.getIdType()));
		ProcessInstance pi = reqDelegate.instantiateProcess(id, inputs, entry.getProcessId());						 
		return pi;
	}
	
	private boolean checkDataConsistency(@NonNull ProcessInstance procInst) {
		boolean hasError = false;
		PROCESSTYPEIDS procId = PROCESSTYPEIDS.valueOf(procInst.getDefinition().getName());
		ProcessStep onlyStep = procInst.getProcessSteps().stream().findAny().get();
		ConstraintWrapper onlyQA = onlyStep.getQAstatus().stream().findAny().get();
		if(procInst.getInput("CRs").size() == 0) {
			System.out.println(String.format("Process Step %s has no input", onlyStep.getName())); 
			hasError = true;
		}
		if (onlyStep.getInput("CRs").size() == 0) {
			System.out.println(String.format("Process %s has no input", procInst.getName()));
			hasError = true;
		}
		if(onlyQA.getEvalResult() == true) {
			System.out.println(String.format("QA Check in step %s is not violated", onlyStep.getName()));
			hasError = true;
		}
		switch(procId) {			// REQs TCs Bugs
		case _TaskWarmup:
			if (!checkOutputs(onlyStep, 4, 0, 0));
			hasError = true;
			break;
		case Task1a:
			if (!checkOutputs(onlyStep, 5, 0, 5));
			hasError = true;
			break;
		case Task1b:
			if (!checkOutputs(onlyStep, 3, 0, 0));
			hasError = true;
			break;
		case Task1c:
			if (!checkOutputs(onlyStep, 7, 13, 2));
			hasError = true;
			break;
		case Task2a:
			if (!checkOutputs(onlyStep, 5, 0, 5));
			hasError = true;
			break;
		case Task2b:
			if (!checkOutputs(onlyStep, 4, 0, 0));
			hasError = true;
			break;
		case Task2c:
			if (!checkOutputs(onlyStep, 7, 14, 2));
			hasError = true;
			break;
		case Task3a:
			if (!checkOutputs(onlyStep, 5, 0, 5));
			hasError = true;
			break;
		case Task3b:
			if (!checkOutputs(onlyStep, 4, 0, 0));
			hasError = true;
			break;
		case Task3c:
			if (!checkOutputs(onlyStep, 7, 14, 4));
			hasError = true;
			break;
		default:
			System.out.println("Unknown ProcessType encountered: "+procId);
			break;
		}
		return hasError;
	}

	private boolean checkOutputs(ProcessStep step, int reqSize, int tcSize, int bugSize) {
		boolean success = step.getOutput(OUTPUTS.TCs.toString()).size()==tcSize 
				&& step.getOutput(OUTPUTS.REQs.toString()).size()==reqSize 
				&& step.getOutput(OUTPUTS.Bugs.toString()).size()==bugSize;
		if (!success) {
			System.out.println(String.format("Process step %s has differing output size",step.getName()));
		}
		return success;
	}
}
