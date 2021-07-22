package impactassessment.ltlcheck;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;
import org.processmining.analysis.ltlchecker.parser.LTLParser;
import org.processmining.analysis.ltlchecker.parser.ParseException;
import org.processmining.framework.log.LogFile;
import org.processmining.framework.log.LogFilter;
import org.processmining.framework.log.LogReader;
import org.processmining.framework.log.LogReaderFactory;
import org.processmining.framework.log.filter.DefaultLogFilter;

import impactassessment.ltlcheck.LTLFormulaProvider.AvailableFormulas;
import impactassessment.ltlcheck.ValidationUtil.ValidationSelection;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chris
 */
@Slf4j
public class RuntimeParser {

	/** constants **/
	private static final String LTL_TEST_FILE = "metadata/running.ltl";
	private static final String PROCESS_LOG_TEST_FILE = "metadata/running.xml";

	public static ArrayList<ValidationResult> testLTLValidation() {
		try {
			LogReader logReader = LogReaderFactory.createInstance(new DefaultLogFilter(LogFilter.FAST),
					LogFile.getInstance(getAbsolutePath(PROCESS_LOG_TEST_FILE)));
			log.debug("" + logReader.getLogSummary().getNumberOfProcessInstances());
			log.debug("" + logReader.getLogSummary().getNumberOfAuditTrailEntries());

			String formulas = fileToString(LTL_TEST_FILE);
			LTLParser p = invokeParser(formulas);
			for (Object o : p.getAttributes()) {
				System.out.println(o.toString());
			}
			RuntimeValidator r = new RuntimeValidator(logReader, p, null,
					Pair.of(ValidationSelection.SPECIAL, "does_John_drive"));

			return r.validate();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public static ArrayList<ValidationResult> checkLTLTrace(AvailableFormulas formulaDefinition,
			ValidationSelection validationSelection) {
		try {
			LogReader logReader = LogReaderFactory.createInstance(new DefaultLogFilter(LogFilter.FAST),
					LogFile.getInstance(getAbsolutePath(PROCESS_LOG_TEST_FILE)));
			log.debug("" + logReader.getLogSummary().getNumberOfProcessInstances());
			log.debug("" + logReader.getLogSummary().getNumberOfAuditTrailEntries());

			// String formulas = fileToString(LTL_TEST_FILE);
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

			RuntimeValidator r = new RuntimeValidator(logReader, p, null, Pair.of(validationSelection, formulaName));

			return r.validate();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	private static String fileToString(String fileName) throws IOException {
		InputStream is = RuntimeParser.class.getResourceAsStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line = null;

		String ls = System.getProperty("line.separator");
		while ((line = reader.readLine()) != null) {
			sb.append(line);
			sb.append(ls);
		}
		sb.deleteCharAt(sb.length() - 1);
		reader.close();

		return sb.toString();
	}

	private static String getAbsolutePath(String fileName) {
		return RuntimeParser.class.getResource(fileName).getPath();
	}

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
