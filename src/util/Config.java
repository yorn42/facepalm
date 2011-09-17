package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Config {

	private String gexpath;

	private ArrayList<String> cookies = new ArrayList<String>();

	private String db_cook_config = "cookie=";

	private String gexfpath_config = "gexfpath=";

	private String facebookProfileURL = "http://www.facebook.com/profile.php?id=";

	public void doConfig(String configFile) throws IOException {
		if (configFile == null) {
			configFile = "config/succulent.conf";
		}
		BufferedReader in = new BufferedReader(new FileReader(configFile));
		String strLine;
		while ((strLine = in.readLine()) != null) {

			if (strLine.toLowerCase().startsWith(db_cook_config)) {
				cookies.add(strLine.substring(db_cook_config.length()));
			} else if (strLine.toLowerCase().startsWith(gexfpath_config)) {
				gexpath = strLine.substring(gexfpath_config.length());
			}
		}
	}

	public String getCookies() {
		StringBuilder sb = new StringBuilder();
		for (String cookie : cookies) {
			sb.append(cookie + ";");
		}
		return sb.toString();
	}

	public String getFacebookProfileURL() {
		return facebookProfileURL;
	}

	public String getGexfPath() {
		return gexpath;
	}
}
