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
 * @author chris
 */
@Slf4j
public class RuntimeParser {

	/**
	 * Validate the passed LTL formula against the process log to be created for the
	 * passed workflow instance.
	 *
	 * @param formulaDefinition The formula to be validated against the workflow.
	 * @param transitionEvent   The transition event from which a process log will
	 *                          be generated.
	 * @return an ArrayList containing the validation results for every single
	 *         process instance generated from the passed workflow instance
	 *         (essentially only one)
	 *
	 */
	public static ArrayList<ValidationResult> checkLTLTrace(AvailableFormulas formulaDefinition,
			TaskStateTransitionEvent transitionEvent) {
		try {
			// retrieve the formula object associated with the parameter formulaDefinition
			LTLFormulaObject formulaObj = LTLFormulaProvider.getFormulaDefinition(formulaDefinition);

			// build the XML process log for the workflow encapsulated by parameter
			// transitionEvent
			LTLProcessInstanceObject piObj = WorkflowDataExtractor.extractWorkflowInformation(transitionEvent);
			String processLogPath = WorkflowXmlConverter.getInstance().processWorkflow(piObj,
					formulaObj.getValidationMode());

			// if the process log creation/expansion succeeded continue
			if (processLogPath != null) {
				LogReader logReader = new LogReader(new DefaultLogFilter(LogFilter.FAST),
						LogFile.getInstance(processLogPath));
				log.debug("" + logReader.getLogSummary().getNumberOfProcessInstances());
				log.debug("" + logReader.getLogSummary().getNumberOfAuditTrailEntries());

				// parse the defined formula
				String formulaStructure = formulaObj.getFormulaDefinition();
				LTLParser p = invokeParser(formulaStructure);
				for (Object o : p.getAttributes()) {
					System.out.println(o.toString());
				}

				String formulaName = formulaDefinition.toString();
				RuntimeValidator r = new RuntimeValidator(logReader, p, null, formulaName);

				return r.validate();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	/**
	 * Invoke the {@link LTLParser} which parses and verifies the formula to be
	 * validated.
	 *
	 * @return the invoked {@link LTLParser} instance
	 */
	private static LTLParser invokeParser(String ltlFormulas) {
		LTLParser localParser = new LTLParser(new ByteArrayInputStream(ltlFormulas.getBytes(StandardCharsets.UTF_8)));
		localParser.init();
		try {
			localParser.parse();
		} catch (ParseException pex) {
			pex.printStackTrace();
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return localParser;
	}
}
