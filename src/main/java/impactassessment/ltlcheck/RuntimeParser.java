package impactassessment.ltlcheck;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.processmining.analysis.ltlchecker.parser.LTLParser;
import org.processmining.analysis.ltlchecker.parser.ParseException;
import org.processmining.framework.log.DefaultLogFilter;
import org.processmining.framework.log.LogFile;
import org.processmining.framework.log.LogFilter;
import org.processmining.framework.log.LogReader;

import impactassessment.ltlcheck.LTLFormulaProvider.AvailableFormulas;
import impactassessment.ltlcheck.convert.WorkflowXmlConverter;
import impactassessment.ltlcheck.util.LTLFormulaObject;
import impactassessment.ltlcheck.util.LTLProcessInstanceObject;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.instance.TaskStateTransitionEvent;

/**
 * This class is responsible for calling the process log creation/expansion
 * process, invoking the {@link LTLParser} for parsing the desired LTL
 * formula(s), and calling the validation routine in the
 * {@link RuntimeValidator} after the aforementioned steps have been completed.
 *
 * @author chris
 */
@Slf4j
public class RuntimeParser {

	/**
	 * Validate the passed LTL formula against the process log to be created for the
	 * passed workflow instance.
	 *
	 * @param workflowID        The ID of the workflow the validation procedure is
	 *                          to be invoked for.
	 * @param formulaDefinition The formula to be validated against the workflow.
	 * @param transitionEvent   The transition event from which a process log will
	 *                          be generated.
	 * @return an ArrayList containing the validation results for every single
	 *         process instance generated from the passed workflow instance
	 *         (essentially only one)
	 *
	 */
	public static ArrayList<ValidationResult> checkLTLTrace(String workflowID, AvailableFormulas formulaDefinition,
			TaskStateTransitionEvent transitionEvent) {
		try {
			// retrieve the formula object associated with the parameter "formulaDefinition"
			LTLFormulaObject formulaObj = LTLFormulaProvider.getFormulaDefinition(formulaDefinition.toString());

			// parse the defined formula(s) -- only once
			if (!formulaObj.isFormulaParsed()) {
				formulaObj.setLTLParserInstance(invokeParser(formulaObj));
			}

			// obtain central parser instance
			LTLParser parser = formulaObj.getLTLParserInstance();

			if (parser != null) {
				// print attributes of current formula
				StringBuilder sb = new StringBuilder();
				sb.append("Attributes of formula ");
				sb.append(formulaDefinition.toString() + ":");
				for (Object o : parser.getAttributes()) {
					sb.append("\n\t" + o.toString());
				}
				log.debug(sb.toString());

				String formulaName = formulaDefinition.toString();

				// build the XML process log for the workflow encapsulated by parameter
				// transitionEvent
				LTLProcessInstanceObject piObj = WorkflowDataExtractor.extractWorkflowInformation(workflowID,
						formulaName, transitionEvent);
				String processLogPath = WorkflowXmlConverter.getInstance().processWorkflow(workflowID, piObj,
						formulaObj.getValidationMode(), formulaObj.getFormulaName());

				// continue if the process log creation/expansion succeeded
				if (processLogPath != null) {
					LogReader logReader = new LogReader(new DefaultLogFilter(LogFilter.FAST),
							LogFile.getInstance(processLogPath));
					log.debug("Nr. of process instances: {}", logReader.getLogSummary().getNumberOfProcessInstances());
					log.debug("Nr. of audit trail entries: {}",
							logReader.getLogSummary().getNumberOfAuditTrailEntries());

					RuntimeValidator r = new RuntimeValidator(logReader, parser, null, formulaName);

					return r.validate();
				}
			}
		} catch (Exception ex) {
			log.error("Unexpected error while preparing for the process log validation routine.", ex);
		}

		return null;
	}

	/**
	 * Create a new {@link LTLParser} instance which parses and verifies the formula
	 * to be validated in a subsequent step.
	 *
	 * @param formulaObj The {@link LTLFormulaObject} containing the string
	 *                   representation of the LTL formula(s) to be parsed.
	 * @return instance of {@link LTLParser}
	 */
	private static LTLParser invokeParser(LTLFormulaObject formulaObj) {
		String formulaDefinition = formulaObj.getFormulaDefinition();
		byte[] formulaBuffer = formulaDefinition.getBytes(StandardCharsets.UTF_8);
		LTLParser localParser = new LTLParser(new ByteArrayInputStream(formulaBuffer));
		localParser.init();

		try {
			localParser.parse();
		} catch (ParseException pex) {
			log.error("Could not parse LTL formula(s) with identifier {}.", formulaObj.getFormulaName(), pex);
			return null;
		} catch (Exception ex) {
			log.error("Unexpected error while parsing LTL formula(s) with identifier {}.", formulaObj.getFormulaName(),
					ex);
			return null;
		}

		return localParser;
	}
}
