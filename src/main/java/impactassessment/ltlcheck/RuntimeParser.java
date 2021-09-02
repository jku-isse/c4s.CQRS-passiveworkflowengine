package impactassessment.ltlcheck;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;
import org.processmining.analysis.ltlchecker.parser.LTLParser;
import org.processmining.analysis.ltlchecker.parser.ParseException;
import org.processmining.framework.log.DefaultLogFilter;
import org.processmining.framework.log.LogFile;
import org.processmining.framework.log.LogFilter;
import org.processmining.framework.log.LogReader;

import impactassessment.ltlcheck.LTLFormulaProvider.AvailableFormulas;
import impactassessment.ltlcheck.convert.WorkflowXmlConverter;
import impactassessment.ltlcheck.util.LTLProcessInstanceObject;
import impactassessment.ltlcheck.util.ValidationUtil.ValidationSelection;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.instance.WorkflowInstance;

/**
 * @author chris
 */
@Slf4j
public class RuntimeParser {

	/**
	 * Validate the passed LTL formula against the process log to be created for the
	 * passed workflow instance.
	 *
	 * @param formulaDefinition   The formula to be validated against the workflow.
	 * @param wfi                 The workflow instance from which a process log
	 *                            will be generated.
	 * @param validationSelection The "validation mode" to be used (validate a
	 *                            specific formula, all defined formulas or a random
	 *                            one).
	 * @return an ArrayList containing the validation results for every single
	 *         process instance generated from the passed workflow instance
	 *         (essentially only one)
	 *
	 */
	public static ArrayList<ValidationResult> checkLTLTrace(AvailableFormulas formulaDefinition, WorkflowInstance wfi,
			ValidationSelection validationSelection) {
		try {
			LTLProcessInstanceObject piObj = WorkflowDataExtractor.extractBasicWorkflowInformation(wfi);
			String processLogPath = WorkflowXmlConverter.getInstance().processWorkflow(piObj);

			if (processLogPath != null) {
				LogReader logReader = new LogReader(new DefaultLogFilter(LogFilter.FAST),
						LogFile.getInstance(processLogPath));
				log.debug("" + logReader.getLogSummary().getNumberOfProcessInstances());
				log.debug("" + logReader.getLogSummary().getNumberOfAuditTrailEntries());

				String formulaStructure = LTLFormulaProvider.getFormulaDefinition(formulaDefinition);
				LTLParser p = invokeParser(formulaStructure);
				for (Object o : p.getAttributes()) {
					System.out.println(o.toString());
				}

				// Only if ValidationSelection.SPECIAL has been selected, a certain formula
				// should be evaluated meaning that the formulaDefinition is mapped to an actual
				// valid formula name.
				// Otherwise (e.g. ValidationSelection.ANY or ALL), no formula name must be
				// passed on to the RuntimeValidator as either an arbitrary formula is selected
				// for evaluation or all formulas are being evaluated.
				String formulaName = "";
				if (validationSelection.equals(ValidationSelection.SPECIAL)) {
					formulaName = formulaDefinition.toString();
				}

				RuntimeValidator r = new RuntimeValidator(logReader, p, null,
						Pair.of(validationSelection, formulaName));

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
