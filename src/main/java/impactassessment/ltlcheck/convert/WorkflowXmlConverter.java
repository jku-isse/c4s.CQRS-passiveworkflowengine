package impactassessment.ltlcheck.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import impactassessment.ltlcheck.util.LTLProcessInstanceObject;
import impactassessment.ltlcheck.util.LTLTaskObject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chris
 */
@Slf4j
public class WorkflowXmlConverter {

	private static WorkflowXmlConverter instance = null;

	// HashMap containing the workflowIDs of workflows for which a process log has
	// already been created. The workflowIDs are associated with the respective file
	// paths of the created process logs.
	private static ConcurrentHashMap<String, String> createdProcessLogs = new ConcurrentHashMap<>();

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

	public String processWorkflow(LTLProcessInstanceObject piObj) {
		String xmlWorkflow = buildXmlWorkflowRepresentation(piObj);
		Document xmlDoc = createXmlDocument(xmlWorkflow);
		Pair<Boolean, String> resultPair = writeDocumentToFS(xmlDoc, piObj.getWorkflowID());
		if (resultPair.getLeft()) {
			if (!createdProcessLogs.containsKey(piObj.getWorkflowID())) {
				createdProcessLogs.put(piObj.getWorkflowID(), resultPair.getRight());
				return resultPair.getRight();
			} else {
				return createdProcessLogs.get(piObj.getWorkflowID());
			}
		}
		return null;
	}

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

	private static Pair<Boolean, String> writeDocumentToFS(Document xmlDoc, String workflowID) {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;

		try {
			transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(xmlDoc);
			String filePath = WorkflowXmlUtility.tempBaseDir + WorkflowXmlUtility.WF_PROCESS_LOG_PREFIX + workflowID
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
}
