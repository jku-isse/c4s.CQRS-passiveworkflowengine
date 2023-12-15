package at.jku.isse.passiveprocessengine.tests;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class TestUtils {

	public static void assertAllConstraintsAreValid(ProcessInstance proc) {
		proc.getProcessSteps().stream()
		.peek(td -> System.out.println("Visiting Step: "+td.getName()))
		.forEach(td -> {
			td.getDefinition().getInputToOutputMappingRules().entrySet().stream().forEach(entry -> {
				InstanceType type = td.getInstance().getProperty("crd_datamapping_"+entry.getKey()).propertyType().referencedInstanceType();
				ConsistencyRuleType crt = (ConsistencyRuleType)type;
				assertTrue(ConsistencyUtils.crdValid(crt));
				String eval = (String) crt.ruleEvaluations().get().stream()
						.map(rule -> ((Rule)rule).result()+"" )
						.collect(Collectors.joining(",","[","]"));
				System.out.println("Checking "+crt.name() +" Result: "+ eval);
			});
			ProcessDefinition pd = td.getProcess() !=null ? td.getProcess().getDefinition() : (ProcessDefinition)td.getDefinition();
			td.getDefinition().getQAConstraints().stream().forEach(entry -> {
				//InstanceType type = td.getInstance().getProperty(ProcessStep.getQASpecId(entry, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td.getDefinition()))).propertyType().referencedInstanceType();
				String id = ProcessStep.getQASpecId(entry, pd);
				ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) td.getInstance().getPropertyAsMap(ProcessStep.CoreProperties.qaState.toString()).get(id));
				if (cw == null) {
					System.out.println("No expected constraint wrapper available found for: "+id);
				} else if (cw.getCr() == null) {
					System.out.println("QAConstraint never evaluated for: "+id);
				} else {
					ConsistencyRuleType crt = (ConsistencyRuleType)cw.getCr().getInstanceType();
					assertTrue(ConsistencyUtils.crdValid(crt));
					String eval = (String) crt.ruleEvaluations().get().stream()
								.map(rule -> ((Rule)rule).result()+"" )
								.collect(Collectors.joining(",","[","]"));
					System.out.println("Checking "+crt.name() +" Result: "+ eval);
				}
			});
			for (Conditions condition : Conditions.values()) {
				if (td.getDefinition().getCondition(condition).isPresent()) {
					InstanceType type = td.getInstance().getProperty(condition.toString()).propertyType().referencedInstanceType();
					ConsistencyRuleType crt = (ConsistencyRuleType)type;
					assertTrue(ConsistencyUtils.crdValid(crt));
					String eval = (String) crt.ruleEvaluations().get().stream()
							.map(rule -> ((Rule)rule).result()+"" )
							.collect(Collectors.joining(",","[","]"));
					System.out.println("Checking "+crt.name() +" Result: "+ eval);
				}	
			}
	});
		proc.getDefinition().getPrematureTriggers().entrySet().stream()
		.forEach(entry -> {
			if (entry.getValue() != null) {
				String ruleName = ProcessInstance.generatePrematureRuleName(entry.getKey(), proc.getDefinition());
				Collection<InstanceType> ruleDefinitions = proc.getInstance().workspace.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE).subTypes();
		        if(! ruleDefinitions.isEmpty() && !(ruleDefinitions.stream().filter(inst -> !inst.isDeleted).count() == 0)) {
		        	for(InstanceType crt: ruleDefinitions.stream().filter(inst -> !inst.isDeleted).collect(Collectors.toSet() )){
		        		if (crt.name().equalsIgnoreCase(ruleName)) {
		        			assertTrue(ConsistencyUtils.crdValid((ConsistencyRuleType)crt));
		        			String eval = (String) ((ConsistencyRuleType)crt).ruleEvaluations().get().stream()
									.map(rule -> ((Rule)rule).result()+"" )
									.collect(Collectors.joining(",","[","]"));
							System.out.println("Checking "+crt.name() +" Result: "+ eval);
		        		}
		        	}
		        }
			}
		});
	}
	
	public static void printFullProcessToLog(ProcessInstance proc) {
		printProcessToLog(proc, " ");
	}
	
 	public static void printProcessWithRepairs(ProcessInstance wfi) {
 		wfi.getProcessSteps().stream()
        .filter(wft -> !wft.areConstraintsFulfilled(ProcessStep.CoreProperties.qaState.toString()))
        .peek(wft -> System.out.println(String.format("Step: %s ", wft.getName())))
        .flatMap(wft -> wft.getQAstatus().stream().filter(cwrapper -> cwrapper.getEvalResult() == false) )
        .peek(cwrapper -> System.out.println(String.format("Constraint: %s ", cwrapper.getSpec().getHumanReadableDescription())))
        .peek(cwrapper -> System.out.println(String.format("ARL: %s ", cwrapper.getSpec().getConstraintSpec())))
        .map(cwrapper -> RuleService.repairTree(cwrapper.getCr()))
        .forEach(rnode -> { StringBuffer sb = new StringBuffer();
        	compileRestrictedRepairTree(rnode, 0, sb);
        	System.out.println(sb.toString());
        });
 	}
	
	public static void printProcessToLog(ProcessInstance proc, String prefix) {
		
		System.out.println(prefix+proc.toString());
		String nextIndent = "  "+prefix;
		proc.getProcessSteps().stream()
			.sorted(new ProcessStep.CompareBySpecOrder())
			.forEach(step -> {
			if (step instanceof ProcessInstance) {
				printProcessToLog((ProcessInstance) step, nextIndent);
			} else {
				System.out.println(nextIndent+step.toString());
			}
		});
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(nextIndent+dni.toString()));
	}
	
	public static void printRepairActions(RepairNode rnode) {
		StringBuffer sb = new StringBuffer();
		compileRestrictedRepairTree(rnode, 1, sb);
		System.out.println(sb.toString());

	}
	
	 public static void compileRestrictedRepairTree(RepairNode node, int position, StringBuffer printInto) {
	    	String treeLevel = "\n";
	    	for (int i = 0; i < position; i++) 
	    		treeLevel = treeLevel.concat(" -- ");
	    	if (node instanceof AbstractRepairAction) {
	    		AbstractRepairAction ra = (AbstractRepairAction)node;
	    		RestrictionNode rootNode =  ra.getValue()==UnknownRepairValue.UNKNOWN && ra.getRepairValueOption().getRestriction() != null ? ra.getRepairValueOption().getRestriction().getRootNode() : null;
	    		if (rootNode != null) {
	    			printInto.append(treeLevel.concat(compileRestrictedRepair(ra,rootNode.printNodeTree(false, 40))));
	    			printInto.append("\n"+ rootNode.toTreeString(40));
	    		} else	
	    			printInto.append(treeLevel.concat(node.toString()));
	    	} else
	    		printInto.append(treeLevel.concat(node.toString()));
	    	for (RepairNode child : node.getChildren()) {
	    		compileRestrictedRepairTree(child, position + 1, printInto);
	    	}
	    }
	    
	    
	    public static String compileRestrictedRepair(AbstractRepairAction ra, String restriction) {
			String target = ra.getElement() != null ? ((Instance)ra.getElement()).name() : "";
			StringBuffer list = new StringBuffer();
			switch(ra.getOperator()) {
			case ADD:							 
				list.append(String.format("Add to %s of ", ra.getProperty()));
				list.append(target);
				list.append(restriction);
				break;
			case MOD_EQ:
			case MOD_GT:
			case MOD_LT:
			case MOD_NEQ:				
				list.append(String.format("Set the %s of ", ra.getProperty()));
				list.append(target);			
				list.append(" to");
				list.append(restriction);
				break;
			case REMOVE:					
				list.append(String.format("Remove from %s of ", ra.getProperty()));
				list.append(target);
				list.append(restriction);
				break;
			default:
				break;		
			}
			return list.toString();
		}
}
