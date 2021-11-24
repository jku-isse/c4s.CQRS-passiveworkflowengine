package impactassessment.query;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class JsonFileHistoryLogEventLogger implements IHistoryLogEventLogger {
	PrintWriter out;
	boolean firstEntry = true;
	
	public JsonFileHistoryLogEventLogger(String logLocation) {
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(logLocation+"/eventlog_"+System.currentTimeMillis()+".json", true)));
			out.println("[");
		    Thread closingHook = new Thread(() -> { out.println("]"); 
		    										out.close(); 
		    										});
		    Runtime.getRuntime().addShutdownHook(closingHook);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void log(String entryAsJson) {
		if (firstEntry) {
			out.println(entryAsJson);
			firstEntry=false;
		} else {
			out.println(","+entryAsJson);
		}
	}
	
	

}
