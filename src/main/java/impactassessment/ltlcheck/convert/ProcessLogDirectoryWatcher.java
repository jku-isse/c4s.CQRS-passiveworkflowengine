package impactassessment.ltlcheck.convert;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

/**
 * This class watches the directory holding the created process logs for
 * changes. By default, this directory is the system specific temp-folder.
 *
 * @author chris
 */
@Slf4j
public class ProcessLogDirectoryWatcher {

	/** constants **/
	private static final int DELETION_THRESHOLD = 50;

	/**
	 * Start the process log directory watcher.
	 *
	 * @param cleanupBeforeExecution If set to true, a thread will be started making
	 *                               sure that all created process logs of a
	 *                               previous validation session are deleted. This
	 *                               can be useful as the <em>ProM-framework</em>
	 *                               sometimes keeps file handles which prevent the
	 *                               installed shutdown hook from automatically
	 *                               removing created process logs. Albeit remaining
	 *                               process logs will not disturb the validation
	 *                               routine (if process logs for a workflow are
	 *                               already present (e.g. logs with the exact same
	 *                               name are present in the FS), the
	 *                               {@link WorkflowXmlConverter} will simply
	 *                               override them), its better to cleanup properly
	 *                               before starting another validation session.
	 */
	public static void start(boolean cleanupBeforeExecution) throws Exception {
		Path watchPath = Paths.get(WorkflowXmlUtility.tempBaseDir);
		if (watchPath == null) {
			throw new UnsupportedOperationException("Temp-directory could not be found.");
		}

		// invoke the cleanup worker
		if (cleanupBeforeExecution) {
			new ProcessLogCleanupWorker().start();
		}

		WatchService watcher = watchPath.getFileSystem().newWatchService();
		WatchQueueReader reader = new WatchQueueReader(watcher);

		watchPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

		ExecutorService cachedThreadPool = Executors.newFixedThreadPool(1);
		cachedThreadPool.execute(reader);

		Runtime.getRuntime().addShutdownHook(new ProcessLogDirWatcherShutdownHook());
	}

	private static class WatchQueueReader implements Runnable {

		private WatchService watcher;
		private int processLogCount;

		public WatchQueueReader(WatchService watcher) {
			this.watcher = watcher;
			this.processLogCount = 0;
		}

		@Override
		public void run() {
			while (true) {
				WatchKey key = null;

				if (processLogCount >= DELETION_THRESHOLD) {
					PathMatcher pathMatcher = FileSystems.getDefault()
							.getPathMatcher("regex:" + WorkflowXmlUtility.WF_PROCESS_LOG_PREFIX + "\\w+\\"
									+ WorkflowXmlUtility.WF_PROCESS_LOG_SUFFIX);
					try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(new File("..").toPath(),
							pathMatcher::matches)) {
						Iterator<Path> iter = dirStream.iterator();
						while (iter.hasNext()) {
							Path p = iter.next();
							iter.remove();

							p.toFile().delete();
						}
					} catch (IOException e) {
						log.error("Error while deleting process logs.");
					}
				}

				try {
					key = watcher.take();
				} catch (InterruptedException ex) {
					log.error("An error occurred while processing a process log file operation.");
				}

				for (WatchEvent<?> event : key.pollEvents()) {
					Path p = (Path) event.context();
					String fileName = p.getFileName().toString();
					if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE
							&& fileName.contains(WorkflowXmlUtility.WF_PROCESS_LOG_PREFIX)) {
						processLogCount--;
					} else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
							&& fileName.contains(WorkflowXmlUtility.WF_PROCESS_LOG_PREFIX)) {
						processLogCount++;
					}
				}

				boolean valid = key.reset();
				if (!valid) {
					break;
				}
			}
			log.info("Discard process log directory watcher.");
		}
	}

	/**
	 * Shutdown hook responsible for removing all process logs created during a
	 * validation session.
	 */
	private static class ProcessLogDirWatcherShutdownHook extends ProcessLogCleanupWorker {

		public ProcessLogDirWatcherShutdownHook() {
			super("ProcessLogDirWatcherShutdownHook");
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
			log.info(cleanedFiles + " created process log file(s) have been removed.");
		}
	}
}
