package impactassessment.ltlcheck.framework;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import org.processmining.analysis.ltlchecker.SetsSet;
import org.processmining.analysis.ltlchecker.parser.Attribute;
import org.processmining.analysis.ltlchecker.parser.LTLParser;
import org.processmining.framework.log.AuditTrailEntries;
import org.processmining.framework.log.AuditTrailEntry;
import org.processmining.framework.log.LogReader;
import org.processmining.framework.log.LogSummary;
import org.processmining.framework.log.ProcessInstance;
import org.processmining.framework.ui.Progress;

/**
 * Custom implementation of {@link SetsSet} that does not require any
 * GUI-components.
 *
 * @author chris
 */
public class CustomSetsSet extends SetsSet {

	/** constants **/
	private static final String KEY_WORKFLOW_MODEL_ELEM = "WorkflowModelElement";
	private static final String KEY_EVENT_TYPE = "EventType";
	private static final String KEY_ORIGINATOR = "Originator";
	private static final String KEY_TIMESTAMP = "Timestamp";

	/**
	 * The set containing the sets of the process instance sets.
	 */
	TreeMap<String, HashSet<String>> piSets;

	/**
	 * The names of the sets in piSets.
	 */
	ArrayList<String> piNames;

	/**
	 * The set containing the sets of the audit trail entries.
	 */
	TreeMap<String, HashSet<String>> ateSets;

	/**
	 * The names of the sets in ateSets.
	 */
	ArrayList<String> ateNames;

	/**
	 * If only standard sets are used, set flag to save time.
	 */
	boolean standardSetsOnly;

	public CustomSetsSet(LTLParser parser, LogSummary log) {
		super(parser, log);

		Attribute attr;
		String name;

		// If no sets are created at all, only standard ones will be used.
		standardSetsOnly = true;

		// collections for process instances
		piSets = new TreeMap<>();
		piNames = new ArrayList<>();

		// collections for audit trail entries
		ateSets = new TreeMap<>();
		ateNames = new ArrayList<>();

		ArrayList<?> as = parser.getAttributes();
		Iterator<?> iter = as.iterator();

		while (iter.hasNext()) {
			attr = (Attribute) iter.next();

			// only create sets for attributes with type 'set'
			if (attr.getType() == Attribute.SET) {

				// process instance scope
				if (attr.getScope() == Attribute.PI) {

					// remove prefix 'pi.'
					name = attr.getAttributeId().substring(3);
					piSets.put(name, new HashSet<>());
					piNames.add(name);

					// sets created in the scope of process instances are never standard sets
					standardSetsOnly = false;
				} else {
					// audit trail entry scope

					// remove prefix 'ate.'
					name = attr.getAttributeId().substring(4);

					if (name.equals(KEY_WORKFLOW_MODEL_ELEM)) {
						String[] ss = log.getModelElements();
						HashSet<String> hs = new HashSet<>(ss.length);

						for (String element : ss) {
							hs.add(element);
						}

						ateSets.put(name, hs);
						ateNames.add(name);
					} else if (name.equals(KEY_EVENT_TYPE)) {
						String[] ss = log.getEventTypes();
						HashSet<String> hs = new HashSet<>(ss.length);

						for (String element : ss) {
							hs.add(element);
						}

						ateSets.put(name, hs);
						ateNames.add(name);
					} else if (name.equals(KEY_ORIGINATOR)) {
						String[] ss = log.getOriginators();
						HashSet<String> hs = new HashSet<>(ss.length);

						for (String element : ss) {
							hs.add(element);
						}

						ateSets.put(name, hs);
						ateNames.add(name);
					} else {
						// data element set attribute

						ateSets.put(name, new HashSet<String>());
						ateNames.add(name);

						// there is at least one non-standard set to create
						standardSetsOnly = false;
					}
				}
			}
		}
	}

	@Override
	public boolean standardSetsOnly() {
		return standardSetsOnly;
	}

	@Override
	public HashSet<String> getSet(String name, int scope) {
		String prunedName;

		// process instance scope
		if (scope == Attribute.PI) {

			// remove prefix 'pi.'
			prunedName = name.substring(3);
			if (piNames.contains(prunedName)) {
				return piSets.get(prunedName);
			}
		} else {
			// audit trail entry scope

			// remove prefix 'ate.'
			prunedName = name.substring(4);
			if (ateNames.contains(prunedName)) {
				return ateSets.get(prunedName);
			}
		}

		return null;
	}

	@Override
	public void fill(LogReader log, Progress p) {

		log.reset();

		if (!standardSetsOnly) {
			while (log.hasNext()) {

				ProcessInstance pi = log.next();
				fillPiSets(pi);

				AuditTrailEntries ates = pi.getAuditTrailEntries();

				while (ates.hasNext()) {
					AuditTrailEntry ate = ates.next();
					fillAteSets(ate);
				}
			}
		}
	}

	/**
	 * Fill the pi sets with the data-elements of a process instance.
	 *
	 * @param pi The process instance to fill the pi sets with.
	 */
	private void fillPiSets(ProcessInstance pi) {
		Iterator<String> iter = piNames.iterator();
		String name = "";
		HashSet<String> setOfName;

		while (iter.hasNext()) {
			name = iter.next();

			if (pi.getData().containsKey(name)) {
				setOfName = piSets.get(name);
				setOfName.add((String) pi.getData().get(name));
			}
		}
	}

	/**
	 * Fill the ate sets with the data-elements of a audit trail entry.
	 *
	 * @param ate The audit trail entry to fill the ate sets with.
	 */
	private void fillAteSets(AuditTrailEntry ate) {
		Iterator<String> iter = ateNames.iterator();
		String name = "";
		HashSet<String> setOfName;

		while (iter.hasNext()) {
			name = iter.next();

			if (!isStandard(name)) {
				if (name.equals(KEY_TIMESTAMP)) {
					setOfName = ateSets.get(name);
					setOfName.add(ate.getTimestamp().toString());
				} else {
					if (ate.getData().containsKey(name)) {
						setOfName = ateSets.get(name);
						setOfName.add((String) ate.getData().get(name));
					}
				}
			}
			// nothing to do (sets have already been created)
		}
	}

	/**
	 * Determines if the passed name is the name of a standard set.
	 *
	 * @param name The name of an attribute.
	 *
	 * @return If name is either WorkflowModelElement, EventType or Originator,
	 *         return true, else false.
	 */
	private boolean isStandard(String name) {
		return (name.equals("WorkflowModelElement") || name.equals("EventType") || name.equals("Originator"));
	}
}
