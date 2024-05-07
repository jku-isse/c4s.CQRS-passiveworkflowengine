package at.jku.isse.passiveprocessengine.frontend.rule;

import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.arl.evaluator.EvaluationNode;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ARLPlaygroundEvaluator {

	private static Workspace playground = null;
	
	public static Set<ResultEntry> evaluateRule(Workspace ws, InstanceType type, String id, String arlString, boolean removeRule) throws ProcessException {
		assert(ws != null);
		initPlayground(ws);
		ProcessException pex = new ProcessException("Errors while evaluating: "+arlString);
		ConsistencyRuleType crt = null;
		boolean ruleExisted = false;
		try {
			crt = ConsistencyRuleType.consistencyRuleTypeExists(playground, id, playground.its(type), arlString);
			if (crt != null) {
				ruleExisted = true;
			} else {
				crt = ConsistencyRuleType.create(playground, playground.its(type), id, arlString);
				playground.concludeTransaction();
			}
			if (crt.hasRuleError()) {
				pex.getErrorMessages().add(crt.ruleError());
				throw pex;
			}
			// now evaluate on instances:
			Set<ResultEntry> result = 
					(Set<ResultEntry>) crt.ruleEvaluations().get().stream()
					.map(rule -> {
						ConsistencyRule r = (ConsistencyRule)rule;
						Instance ctx = r.contextInstance();
						String error = r.hasEvaluationError() ? r.evaluationError() : null;
						Boolean bresult = r.hasEvaluationError() ? null : Boolean.valueOf(r.result());
						EvaluationNode node = RuleService.evaluationTree(r);
						return new ResultEntry(ctx, bresult, error, node, r);
					})
					.collect(Collectors.toSet());
			return result;
		} catch (Exception e) {
			if (!(e instanceof ProcessException))  
				pex.getErrorMessages().add(e.getMessage());
			throw e;
		} finally {
			if (crt != null && !ruleExisted && ( removeRule || crt.hasRuleError()) ) {
				crt.delete();
				playground.concludeTransaction();
			}
		}
	}
	
	private static void initPlayground(Workspace parent) {
		if (playground == null) {
			//log.info("Creating rule evaluation workspace: 'ArlPlayground'");
			//playground = parent.create("ArlPlayground", parent, new User("ARLPlaygroundEvaluatorBot"), new Tool("ArlPlayground", "V1"), true, false);
			log.debug("Using process workspace as rule playground to allow for lazy loading of artifacts");
			playground = parent;
		}
	}
	
	 @Data
	 public static class ResultEntry {
	    	final Instance instance;
	    	final Boolean result;
	    	final String error;
	    	final EvaluationNode rootNode;
	    	final ConsistencyRule ruleInstance;
	    	
	    	
	    }
}
