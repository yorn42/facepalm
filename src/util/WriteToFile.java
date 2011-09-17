package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.io.FileUtils;

public class WriteToFile {

	@SuppressWarnings("static-access")
	public void writeToFile(String content, String file) {
		Writer fw = null;
		FileUtils fu = new FileUtils();
		try {
			File dir = new File(file);
			dir.mkdirs();
			fw = new FileWriter(dir.getAbsolutePath() + "/fb.gexf");
			fw.write(content);
			fu.copyFileToDirectory(new File("config/GexfExplorer1.0.swf"), dir);
			fu.copyFileToDirectory(new File("config/index.html"), dir);
		} catch (IOException e) {
			System.err.println("[X] Could not write to file!!");
			e.printStackTrace();
		} finally {
			if (fw != null)
				try {
					fw.close();
				} catch (IOException e) {
				}
		}
	}
}
