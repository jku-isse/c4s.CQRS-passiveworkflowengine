package at.jku.isse.passiveprocessengine.frontend.oclx;

import java.util.ArrayList;
import java.util.List;

import at.jku.isse.ide.assistance.CodeActionExecuter;
import at.jku.isse.passiveprocessengine.frontend.botsupport.GeneratedRulePostProcessor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLExtractor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IterativeRepairer {

	private final CodeActionExecuterProvider provider;
	
	
	public IterationResult checkResponse(String context, String prompt,
			String answer, int iteration) {
		var result = new IterativeRepairer.IterationResult(iteration, prompt, answer);
		// check for errors, correct OCL?
		var ocl = new OCLExtractor(answer).extractOCLorNull();
		if (ocl == null) {
			return result;
		}
		result.setOclString(ocl);
		// if yes --> correct OCLX?
		
		var oclx = GeneratedRulePostProcessor.init(ocl).getProcessedRule();
		oclx = IterativeRepairer.wrapInOCLX(oclx, context);
		result.setOclxString(oclx);
		
		CodeActionExecuter executer;
		try {
			executer = provider.buildExecuter(oclx);
		} catch (Exception e) {
			result.addError(e.getMessage());
			return result;
		}
		// there are problems:
		executer.checkForIssues();
		var issues = executer.getProblems();
		if (issues.isEmpty()) {
			return result;
		}
		issues.forEach(issue -> result.addError(issue.getMessage()));
		for (int i = 0; i< 5; i++) { // max 5 rounds of repairs
			// repairs?
			executer.executeFirstExecutableRepair();		
			var repair = executer.getExecutedCodeAction();
			// if yes --> autoexecuted?
			if (repair == null) { // autorepair failed
				result.setRemainingError(executer.getProblems().get(0).getMessage());
				return result;
			}
			// else autoexecuted repair
			oclx = executer.getRepairedOclxConstraint();
			result.setFixedOclString(executer.getRepairedExpression());
			result.setFixedOclxString(oclx);
			executer = provider.buildExecuter(oclx); // we need a new executer (to parse the new text)
			executer.checkForIssues(); // any remaining issues?
			if (executer.getProblems().isEmpty()) {
				return result;
			} // else try repair next error
		}
		
		// if  remaining errors
		result.setRemainingError(executer.getProblems().get(0).getMessage());
		return result;
	}
	
	
	
	
	public static String wrapInOCLX(String constraint, String context) {
		return "rule TestRule {\r\n"
				+ "					context: "+context+"\r\n"
				+ "					expression: "+constraint+" \r\n"
				+ "				}";	
	}
	
	@Data
	public static class IterationResult {
		
		final int iteration;
		final String prompt;
		final String rawResponse;
		String oclString = null;
		String fixedOclString = null;
		String oclxString = null;
		List<String> errors = new ArrayList<>();
		String fixedOclxString = null;
		String remainingError = null;
	
		public void addError(String message) {
			errors.add(message);
		}

		public String toRepairInfoOnlyString() {
			return "[oclString=" + oclString + ", \r\nfixedOclString=" + fixedOclString + ", \r\noclxString="
					+ oclxString + ", \r\nerrors=" + errors + ", \r\nfixedOclxString=" + fixedOclxString + ", \r\nremainingError="
					+ remainingError + "]";
		}
		
		
	}

}
