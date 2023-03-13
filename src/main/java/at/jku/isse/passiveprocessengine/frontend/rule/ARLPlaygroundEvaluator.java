package at.jku.isse.passiveprocessengine.frontend.rule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Tool;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.frontend.ui.ARLPlaygroundView;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.Data;

public class ARLPlaygroundEvaluator {

	private static Workspace playground = null;
	
	public static Set<ResultEntry> evaluateRule(Workspace ws, InstanceType type, String id, String arlString) throws ProcessException {
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
						return new ResultEntry(ctx, bresult, error, r);
					})
					.collect(Collectors.toSet());
			return result;
		} catch (Exception e) {
			if (!(e instanceof ProcessException))  
				pex.getErrorMessages().add(e.getMessage());
			throw e;
		} finally {
			if (crt != null && !ruleExisted) {
				crt.delete();
				playground.concludeTransaction();
			}
		}
	}
	
	private static void initPlayground(Workspace parent) {
		if (playground == null) {
			playground = parent.create("ArlPlayground", parent, new User("ARLPlaygroundEvaluatorBot"), new Tool("ArlPlayground", "V1"), true, false);
		}
	}
	
	 @Data
	 public static class ResultEntry {
	    	final Instance instance;
	    	final Boolean result;
	    	final String error;
	    	final ConsistencyRule ruleInstance;
	    	
	    }
}
