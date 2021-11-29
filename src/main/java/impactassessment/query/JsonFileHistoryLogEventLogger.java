package impactassessment.query;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonFileHistoryLogEventLogger implements IHistoryLogEventLogger {
	PrintWriter out;
	boolean firstEntry = true;
	
	public JsonFileHistoryLogEventLogger(String logLocation) {
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(logLocation+"/eventlog_"+System.currentTimeMillis()+".json", true)));
			out.println("[");
			out.flush();
			log.info("Opened file FileWriter for Logging History"); 
		    Thread closingHook = new Thread(() -> { out.println("]"); 
		    										out.close(); 
		    										log.info("Closed file outputstream"); 
		    										});
		    Runtime.getRuntime().addShutdownHook(closingHook);
		} catch (IOException e) {
			log.error("Error closing filestream: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public void log(List<String> entriesAsJson) {
		entriesAsJson.forEach(entryAsJson -> log(entryAsJson));
		out.flush();
	}
	
	protected void log(String entryAsJson) {
		if (firstEntry) {
			out.println(entryAsJson);
			firstEntry=false;
		} else {
			out.println(","+entryAsJson);
		}
	}

}
