package impactassessment.ltlcheck.util;

import impactassessment.ltlcheck.LTLValidationManager;
import impactassessment.ltlcheck.convert.WorkflowXmlConverter;

/**
 * Utility class for the validation process.
 *
 * @author chris
 */
public class ValidationUtil {

	/**
	 * This enum defines the possible processing modes for the validation routine.
	 **/
	public enum ValidationMode {
		/**
		 * TRACE -> Indicates that a process log is to be continuously expanded in order
		 * to create a complete trace of a workflow which represents its complete change
		 * history (e.g. an "activity trace"). This allows to continuously check if
		 * trace violations occurred and if they are repaired again (for example: a
		 * dependent task is complete even though its parent task's state changes from
		 * complete to active). For trace oriented validation runs a special result
		 * analysis is provided.
		 */
		TRACE,
		/**
		 * STATIC -> Indicates that a process log can be overwritten with every new
		 * event invoking the validation routine (e.g. the default behavior of the
		 * {@link WorkflowXmlConverter} in order to statically check if a certain
		 * condition associated with a workflow holds (for example: check if the output
		 * condition of a workflow is satisfied after every trigger event). For static
		 * validation runs the simple (history oriented) validation result analysis
		 * provided by the {@link LTLValidationManager} suffices.
		 */
		STATIC
	}
}
