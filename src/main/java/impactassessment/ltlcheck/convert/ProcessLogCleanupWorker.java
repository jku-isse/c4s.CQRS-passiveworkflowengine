package impactassessment.ltlcheck.convert;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread which can be used to clear all process logs created during a
 * validation session. Derived instances of this class will be used in the
 * {@link ProcessLogDirectoryWatcher}.
 *
 * @author chris
 */
@Slf4j
public class ProcessLogCleanupWorker extends Thread {

	protected static final String processLogRegex = WorkflowXmlUtility.WF_PROCESS_LOG_PREFIX + "(.*)"
			+ WorkflowXmlUtility.WF_PROCESS_LOG_SUFFIX;

	public ProcessLogCleanupWorker(String threadName) {
		super(threadName);
	}

	public ProcessLogCleanupWorker() {
		super("ProcessLogCleanupWorker");
	}

	@Override
	public void run() {
		int cleanedFiles = 0;
		File[] tempFiles = (new File(WorkflowXmlUtility.tempBaseDir)).listFiles();
		for (File tempFile : tempFiles) {
			if (tempFile.getName().matches(processLogRegex)) {
				if (!tempFile.delete()) {
					tempFile.deleteOnExit();
				}
				cleanedFiles++;
			}
		}
		log.info(cleanedFiles + " process log file(s) created during the previous session have been removed.");
	}
}
