package impactassessment.polarion;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class LoadTestHtmlFromFiles {

	
	public static String ST24837incorrect = "ST-24837incorrect.html";
	public static String ST24837incorrect2 = "ST-24837incorrect2.html";
	public static String ST24837correct = "ST-24837correct.html";
	public static String PATH_PREFIX = "src/test/resources";
	
	public static Optional<String> load_ST24837correct_Description() {
		return load(Path.of("src","test","resources", ST24837correct));
	}
	
	public static Optional<String> load_ST24837incorrect_Description() {
		return load(Path.of("src","test","resources", ST24837incorrect));
	}
	
	public static Optional<String> load_ST24837incorrect2_Description() {
		return load(Path.of("src","test","resources", ST24837incorrect2));
	}
	
	
	private static Optional<String> load(Path path) {
		try {
//			System.out.println(path.toFile().getAbsolutePath() +" exists? "+ path.toFile().exists());
			String content = Files.readString(path, Charset.forName("Cp1252"));
			return Optional.of(content);
		} catch (IOException e) {
			
			e.printStackTrace();
			return Optional.empty();
		}
	}
}
