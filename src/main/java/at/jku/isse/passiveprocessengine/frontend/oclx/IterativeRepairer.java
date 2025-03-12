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
		var ocl = new OCLExtractor(answer).extractOCLorEmpty();
		if (ocl == null) {
			return result;
		}
		result.setOclString(ocl);
		// if yes --> correct OCLX?
		
		ocl = GeneratedRulePostProcessor.init(ocl).getProcessedRule();
		result.setFixedOclString(ocl);
		var oclx = IterativeRepairer.wrapInOCLX(ocl, context);
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
		issues.forEach(issue -> result.addError(issue.getMessage())); // initial errors only, followup errors are not captured here,
		for (int i = 0; i< 15; i++) { // max 15 rounds of repairs
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
		// we also need to remove any linebreaks as for not we cannot handle line numbers
		// for now an ugly/brittle replacement of \r and \n with empty string
		var sanitized = constraint.replace("\r", "").replace("\n", "");
		
		return "rule TestRule {"
				+ "	context: "+context
				+ "	expression: "+sanitized
				+ "	}";	
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
			return "[oclString=\r\n" + oclString + ", \r\nfixedOclString=\r\n" + fixedOclString + ", \r\noclxString=\r\n"
					+ oclxString + ", \r\nerrors\r\n=" + errors + ", \r\nfixedOclxString=\r\n" + fixedOclxString + ", \r\nremainingError=\r\n"
					+ remainingError + "\r\n]";
		}
		
		
	}

}
