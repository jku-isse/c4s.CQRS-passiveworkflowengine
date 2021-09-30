package impactassessment.ltlcheck.convert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import impactassessment.ltlcheck.util.LTLProcessInstanceObject;
import impactassessment.ltlcheck.util.LTLTaskObject;
import impactassessment.ltlcheck.util.LTLWorkflowConstants;
import impactassessment.ltlcheck.util.ValidationUtil;
import impactassessment.ltlcheck.util.ValidationUtil.ValidationMode;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used for deriving an actual process log file from an internal
 * workflow representation.
 *
 * @author chris
 */
@Slf4j
public class WorkflowXmlConverter {

	private static WorkflowXmlConverter instance = null;

	// HashMap containing the workflowIDs of workflows for which a process log has
	// already been created. The workflowIDs are associated with the respective file
	// paths of the created process logs.
	private static ConcurrentHashMap<String, String> createdProcessLogs = new ConcurrentHashMap<>();

	/**
	 * @return an instance of {@link WorkflowXmlConverter}
	 */
	public static WorkflowXmlConverter getInstance() {
		if (instance == null) {
			instance = new WorkflowXmlConverter();
			try {
				ProcessLogDirectoryWatcher.start(true);
			} catch (Exception ex) {
				log.error("Could not start " + ProcessLogDirectoryWatcher.class.getSimpleName() + ".", ex);
			}
		}
		return instance;
	}

	/**
	 * Process the extracted workflow information.
	 *
	 * @param workflowID  The ID of the workflow the validation procedure is to be
	 *                    invoked for.
	 * @param piObj       The {@link LTLProcessInstanceObject} from which a process
	 *                    log should be derived.
	 * @param mode        The desired validation mode (see {@link ValidationUtil}).
	 * @param formulaName The name of the formula to be validated against a process
	 *                    log. The formula name alongside the passed workflowID will
	 *                    be included in the name of a process log to allow multiple
	 *                    process logs per workflow (and thus the undisturbed
	 *                    validation of multiple formulas per workflow).
	 * @return the absolute path to the created process log
	 */
	public String processWorkflow(String workflowID, LTLProcessInstanceObject piObj, ValidationMode mode,
			String formulaName) {
		String processLogID = buildProcessLogID(workflowID, formulaName);
		if (!createdProcessLogs.containsKey(processLogID)) {
			String xmlWorkflow = buildXmlWorkflowRepresentation(piObj);
			Document xmlDoc = createXmlDocument(xmlWorkflow);
			Pair<Boolean, String> resultPair = writeDocumentToFS(xmlDoc, processLogID);
			if (resultPair.getLeft()) {
				createdProcessLogs.put(processLogID, resultPair.getRight());
				return resultPair.getRight();
			}
		} else {
			if (mode.equals(ValidationMode.STATIC)) {
				createdProcessLogs.remove(processLogID);
				return processWorkflow(workflowID, piObj, mode, formulaName);
			} else if (mode.equals(ValidationMode.TRACE)) {
				boolean success = extendProcessLog(piObj, createdProcessLogs.get(processLogID), processLogID);
				if (success) {
					return createdProcessLogs.get(processLogID);
				}
				createdProcessLogs.remove(processLogID);
			}
		}
		return null;
	}

	/**
	 * Build the unique identifier for a process log. This identifier is also part
	 * of the file name of the created process log.
	 *
	 * @param workflowID  The ID of the workflow the validation procedure is to be
	 *                    invoked for.
	 * @param formulaName The name of the formula to be validated against a process
	 *                    log.
	 * @return the process log ID
	 */
	private String buildProcessLogID(String workflowID, String formulaName) {
		return workflowID + "_" + formulaName;
	}

	/**
	 * Create a XML string representation of a process log for the parameter piObj.
	 *
	 * @param piObj {@link LTLProcessInstanceObject} holding all necessary
	 *              information for building the process log string
	 * @return the XML process log string
	 */
	private static String buildXmlWorkflowRepresentation(LTLProcessInstanceObject piObj) {
		StringBuilder sb = new StringBuilder();

		// open workflow log tag
		sb.append(WorkflowXmlUtility.XML_WORKFLOW_LOG_TAG_OPEN);

		// create workflow log attributes
		sb.append(WorkflowXmlUtility.XML_DATA_TAG_OPEN);
		sb.append(WorkflowXmlUtility.buildAttributeTag("info",
				"This process log represents a Jira/Jama workflow object at a certain point in time."));
		sb.append(WorkflowXmlUtility.XML_DATA_TAG_CLOSE);

		// create source information
		sb.append(WorkflowXmlUtility.buildSourceTag("", ""));

		// open process tag
		sb.append(WorkflowXmlUtility.buildProcessTagOpen("converted_workflow"));

		// create process attributes
		sb.append(WorkflowXmlUtility.XML_DATA_TAG_OPEN);
		sb.append(WorkflowXmlUtility.buildAttributeTag("info",
				"The following information encapsulates the whole workflow (tasks, states, etc.)."));
		sb.append(WorkflowXmlUtility.XML_DATA_TAG_CLOSE);

		// open process instance tag
		sb.append(WorkflowXmlUtility.buildProcessInstanceTagOpen(piObj.getWorkflowID()));

		// create process instance attributes
		if (piObj.getAttributes() != null && !piObj.getAttributes().isEmpty()) {
			sb.append(WorkflowXmlUtility.XML_DATA_TAG_OPEN);
			for (Entry<String, String> entry : piObj.getAttributes().entrySet()) {
				sb.append(WorkflowXmlUtility.buildAttributeTag(entry.getKey(), entry.getValue()));
			}
			sb.append(WorkflowXmlUtility.XML_DATA_TAG_CLOSE);
		}

		// create audit trail entries
		if (piObj.getAuditTrailEntries() != null && !piObj.getAuditTrailEntries().isEmpty()) {
			for (LTLTaskObject task : piObj.getAuditTrailEntries()) {
				sb.append(WorkflowXmlUtility.XML_AUDIT_TRAIL_ENTRY_TAG_OPEN);
				if (task.getAttributes() != null && !task.getAttributes().isEmpty()) {
					sb.append(WorkflowXmlUtility.XML_DATA_TAG_OPEN);
					for (Entry<String, String> entry : task.getAttributes().entrySet()) {
						sb.append(WorkflowXmlUtility.buildAttributeTag(entry.getKey(), entry.getValue()));
					}
					sb.append(WorkflowXmlUtility.XML_DATA_TAG_CLOSE);
				}
				sb.append(WorkflowXmlUtility.buildDefaultAuditTrailEntryTags(task.getWorkflowModelElement(),
						task.getEventType(), task.getOriginator()));
				sb.append(WorkflowXmlUtility.XML_AUDIT_TRAIL_ENTRY_TAG_CLOSE);
			}
		}

		// close process instance tag
		sb.append(WorkflowXmlUtility.XML_PROCESS_INSTANCE_TAG_CLOSE);

		// close process tag
		sb.append(WorkflowXmlUtility.XML_PROCESS_TAG_CLOSE);

		// close workflow log tag
		sb.append(WorkflowXmlUtility.XML_WORKFLOW_LOG_TAG_CLOSE);

		return sb.toString();
	}

	/**
	 * Create the XML document from the XML string representation of a process log.
	 *
	 * @param xmlWorkflow The XML string from which an actual XML document should be
	 *                    created.
	 * @return the created XML document
	 */
	private static Document createXmlDocument(String xmlWorkflow) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;

		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(xmlWorkflow)));
			return doc;
		} catch (Exception ex) {
			log.error("Could not create XML-document from workflow.", ex);
		}
		return null;
	}

	/**
	 * Write the created XML document to the file system.
	 *
	 * @param xmlDoc       The XML process log document.
	 * @param processLogID The identifier of the created process log.
	 * @return pair either consisting of value true if the file system write
	 *         operation was successful and the absolute path to the written file or
	 *         false combined with an empty string if the write operation failed
	 */
	private static Pair<Boolean, String> writeDocumentToFS(Document xmlDoc, String processLogID) {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;

		try {
			transformer = transformerFactory.newTransformer(
					new StreamSource(new ByteArrayInputStream(WorkflowXmlUtility.getFormattingXSLT().getBytes())));
			DOMSource source = new DOMSource(xmlDoc);
			String filePath = WorkflowXmlUtility.tempBaseDir + WorkflowXmlUtility.WF_PROCESS_LOG_PREFIX + processLogID
					+ WorkflowXmlUtility.WF_PROCESS_LOG_SUFFIX;
			FileWriter writer = new FileWriter(new File(filePath));
			StreamResult result = new StreamResult(writer);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(source, result);
			return Pair.of(true, filePath);
		} catch (Exception ex) {
			log.error("Could not write XML-document to FS.", ex);
		}
		return Pair.of(false, "");
	}

	/**
	 * Extend a previously created process log by adding the additional audit trail
	 * entries to the already present process instance. This means that every audit
	 * trail entry of the process log will be an element of a single process
	 * instance.
	 *
	 * @param piObj          The {@link LTLProcessInstanceObject} which contains the
	 *                       new audit trail entries to be added to the process log.
	 * @param processLogPath The path to the already existing process log file.
	 * @param processLogID   The unique identifier of the process log and part of
	 *                       its file name (see
	 *                       {@link #writeDocumentToFS(Document, String)}).
	 * @return true if the extension of the process log was successful, false
	 *         otherwise
	 */
	@SuppressWarnings("unused")
	private static boolean extendProcessLogByAddingToPI(LTLProcessInstanceObject piObj, String processLogPath,
			String processLogID) {

		// instantiate document builder factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try (InputStream is = new FileInputStream(processLogPath)) {

			// parse the existing process log file
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document processLog = db.parse(is);

			// obtain process instance
			NodeList piList = processLog.getElementsByTagName(WorkflowXmlUtility.XML_TAG_NAME_PI);
			// as we only create a single process instance per process log, an index based
			// access is justified
			Node processInstance = piList.item(WorkflowXmlUtility.PROCESS_LOG_PI_DOM_INDEX);

			// checks are just for safety to be sure that we modify the process log in
			// the right place
			if (processInstance.getNodeType() == Node.ELEMENT_NODE) {
				String id = processInstance.getAttributes().getNamedItem(WorkflowXmlUtility.XML_ATTRIBUTE_ID)
						.getTextContent();
				if (piObj.getWorkflowID().equals(id)) {

					// add or update the process instance modification time stamp after obtaining
					// its attributes
					NodeList piChildren = processInstance.getChildNodes();

					// the attributes of a process instance can always be found at index 1
					Node n = piChildren.item(WorkflowXmlUtility.PROCESS_LOG_PI_DATA_DOM_INDEX);

					XPathFactory xpathfactory = XPathFactory.newInstance();
					XPath xpath = xpathfactory.newXPath();
					XPathExpression expr = xpath.compile(
							"//Attribute[@name='" + LTLWorkflowConstants.MODIFICATION_TIMESTAMP_IDENTIFIER + "']");
					NodeList exprList = (NodeList) expr.evaluate(processLog, XPathConstants.NODESET);

					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
					SimpleDateFormat sdf = new SimpleDateFormat(LTLWorkflowConstants.PROCESS_LOG_TIMESTAMP_FORMAT);
					String formattedTimestamp = sdf.format(timestamp);

					if (exprList.getLength() == 0) {
						Element piData = (Element) n;

						Element modificationTimestampAttr = processLog
								.createElement(WorkflowXmlUtility.XML_TAG_NAME_ATTRIBUTE);
						modificationTimestampAttr.setAttribute(WorkflowXmlUtility.XML_ATTRIBUTE_NAME,
								LTLWorkflowConstants.MODIFICATION_TIMESTAMP_IDENTIFIER);

						modificationTimestampAttr.appendChild(processLog.createTextNode(formattedTimestamp));
						piData.appendChild(modificationTimestampAttr);
					} else {
						for (int i = 0; i < exprList.getLength(); i++) {
							exprList.item(i).setTextContent(formattedTimestamp);
						}
					}

					// create and store new audit trail entries
					for (LTLTaskObject task : piObj.getAuditTrailEntries()) {

						// create new audit trail entry
						Element auditTrailEntry = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_ATE);

						if (task.getAttributes() != null && !task.getAttributes().isEmpty()) {
							// create new data tag
							Element data = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_DATA);

							// create and add attributes for the current audit trail entry
							for (Entry<String, String> entry : task.getAttributes().entrySet()) {
								Element attributeTag = processLog
										.createElement(WorkflowXmlUtility.XML_TAG_NAME_ATTRIBUTE);
								attributeTag.setAttribute(WorkflowXmlUtility.XML_ATTRIBUTE_NAME, entry.getKey());
								attributeTag.appendChild(processLog.createTextNode(entry.getValue()));
								data.appendChild(attributeTag);
							}

							// add data tag to audit trail entry
							auditTrailEntry.appendChild(data);
						}

						// create and add workflow model element tag
						Element wfModelElem = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_WF_MODEL_ELEM);
						wfModelElem.appendChild(processLog.createTextNode(task.getWorkflowModelElement()));
						auditTrailEntry.appendChild(wfModelElem);

						// create and add event type tag
						Element eventType = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_EVENT_TYPE);
						eventType.appendChild(processLog.createTextNode(task.getEventType()));
						auditTrailEntry.appendChild(eventType);

						// create and add originator tag
						Element originator = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_ORIGINATOR);
						originator.appendChild(processLog.createTextNode(task.getOriginator()));
						auditTrailEntry.appendChild(originator);

						// add new audit trail entry to process instance
						processInstance.appendChild(auditTrailEntry);
					}

					// write the changes to the process log file
					Pair<Boolean, String> resultPair = writeDocumentToFS(processLog, processLogID);
					if (resultPair.getLeft()) {
						return true;
					}
				}
			}
		} catch (Exception ex) {
			log.error("Could not extend process log {}.", processLogPath, ex);
		}
		return false;
	}

	/**
	 * Extend a previously created process log by adding an additional process
	 * instance which contains the new audit trail entries.
	 *
	 * @param piObj          The {@link LTLProcessInstanceObject} which contains the
	 *                       new audit trail entries to be added to the process log.
	 * @param processLogPath The path to the already existing process log file.
	 * @param processLogID   The unique identifier of the process log and part of
	 *                       its file name (see
	 *                       {@link #writeDocumentToFS(Document, String)}).
	 * @return true if the extension of the process log was successful, false
	 *         otherwise
	 */
	private static boolean extendProcessLog(LTLProcessInstanceObject piObj, String processLogPath,
			String processLogID) {

		// instantiate document builder factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try (InputStream is = new FileInputStream(processLogPath)) {

			// parse the existing process log file
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document processLog = db.parse(is);

			// obtain process tag
			NodeList processList = processLog.getElementsByTagName(WorkflowXmlUtility.XML_TAG_NAME_PROCESS);

			// as we only create a single process per process log, an index based access is
			// justified
			Node processNode = processList.item(WorkflowXmlUtility.PROCESS_LOG_PROCESS_DOM_INDEX);

			// checks are just for safety
			if (processNode.getNodeType() == Node.ELEMENT_NODE) {

				// create a new process instance
				Element newPI = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_PI);
				int piCnt = processLog.getElementsByTagName(WorkflowXmlUtility.XML_TAG_NAME_PI).getLength();
				String piName = piObj.getWorkflowID() + "_" + piCnt;
				newPI.setAttribute(WorkflowXmlUtility.XML_ATTRIBUTE_ID, piName);

				if (piObj.getAttributes() != null && !piObj.getAttributes().isEmpty()) {
					// create data tag enclosing the process instance attributes
					Element data = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_DATA);

					// create and add attributes for the new process instance to the enclosing data
					// tag
					for (Entry<String, String> entry : piObj.getAttributes().entrySet()) {
						Element attributeTag = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_ATTRIBUTE);
						attributeTag.setAttribute(WorkflowXmlUtility.XML_ATTRIBUTE_NAME, entry.getKey());
						attributeTag.appendChild(processLog.createTextNode(entry.getValue()));
						data.appendChild(attributeTag);
					}

					// add data tag to process instance
					newPI.appendChild(data);
				}

				// create and store new audit trail entries
				for (LTLTaskObject task : piObj.getAuditTrailEntries()) {

					// create new audit trail entry
					Element auditTrailEntry = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_ATE);

					if (task.getAttributes() != null && !task.getAttributes().isEmpty()) {
						// create new data tag
						Element data = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_DATA);

						// create and add attributes for the current audit trail entry
						for (Entry<String, String> entry : task.getAttributes().entrySet()) {
							Element attributeTag = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_ATTRIBUTE);
							attributeTag.setAttribute(WorkflowXmlUtility.XML_ATTRIBUTE_NAME, entry.getKey());
							attributeTag.appendChild(processLog.createTextNode(entry.getValue()));
							data.appendChild(attributeTag);
						}

						// add data tag to audit trail entry
						auditTrailEntry.appendChild(data);
					}

					// create and add workflow model element tag
					Element wfModelElem = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_WF_MODEL_ELEM);
					wfModelElem.appendChild(processLog.createTextNode(task.getWorkflowModelElement()));
					auditTrailEntry.appendChild(wfModelElem);

					// create and add event type tag
					Element eventType = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_EVENT_TYPE);
					eventType.appendChild(processLog.createTextNode(task.getEventType()));
					auditTrailEntry.appendChild(eventType);

					// create and add originator tag
					Element originator = processLog.createElement(WorkflowXmlUtility.XML_TAG_NAME_ORIGINATOR);
					originator.appendChild(processLog.createTextNode(task.getOriginator()));
					auditTrailEntry.appendChild(originator);

					// add new audit trail entry to new process instance
					newPI.appendChild(auditTrailEntry);
				}

				// add new process instance to process
				processNode.appendChild(newPI);
			}

			// write the changes to the process log file
			Pair<Boolean, String> resultPair = writeDocumentToFS(processLog, processLogID);
			if (resultPair.getLeft()) {
				return true;
			}

		} catch (Exception ex) {
			log.error("Could not extend process log {}.", processLogPath, ex);
		}
		return false;
	}
}
