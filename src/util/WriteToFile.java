package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

public class WriteToFile {

	@SuppressWarnings("static-access")
	/**
	 * Create a user folder, write the content of the fb.gexf into a file and copy all necessary files to display the data into the directory.
	 * @param content
	 * @param file
	 */
	public void setupFiles(String path) {
		FileUtils fu = new FileUtils();
		File dir = new File(path);
		dir.mkdirs();
		try {
			fu.copyFileToDirectory(new File("config/GexfExplorer1.0.swf"), dir);
			fu.copyFileToDirectory(new File("config/index.html"), dir);
			fu.copyFileToDirectory(new File("config/parts.html"), dir);
		} catch (IOException io) {
			System.err
					.println("[X] Could not copy files to display data from config/ directory");
		}
	}

	/**
	 * Write the contents of the gexf files.
	 * @param gexfContent
	 * @param partsContent
	 * @param path
	 */
	public void writeGexf(String gexfContent, String path) {
		writeTo(gexfContent, path + "/fb.gexf", false);
	}
	
	public void writeParts(String partsContent, String path) {
		writeTo(partsContent, path + "/parts.gexf", false);
	}

	/**
	 * Static method to let basically everyone append some stats.
	 * @param stats
	 * @param path
	 */
	public static void writeStatsToFile(Map<String, String> stats, String path) {
		Iterator<Entry<String, String>> it = stats.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pairs = (Map.Entry<String, String>) it
					.next();
			writeTo("<tr><td>" + pairs.getKey() + "</td><td>"
					+ pairs.getValue() + "</td></tr>", path + "/index.html", true);
			writeTo("<tr><td>" + pairs.getKey() + "</td><td>"
					+ pairs.getValue() + "</td></tr>", path + "/parts.html", true);
		}
	}

	/**
	 * Finalize the HTML files.
	 * @param path
	 */
	public void finalizeFiles(String path) {
		String end = "</table></body></html>";
		writeTo(end, path + "/index.html", true);
		writeTo(end, path + "/parts.html", true);
	}

	private static void writeTo(String content, String path, boolean append) {
		try {
			FileWriter fileWritter = new FileWriter(path, append);
			BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
			bufferWritter.write(content);
			bufferWritter.close();
		} catch (IOException io) {
			io.printStackTrace();
		}
	}
}
