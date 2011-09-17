package util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

public class URLGetter implements Callable<String> {

	private URL url = null;
	private InputStream is;
	private InputStreamReader isr;
	private BufferedReader r;
	private String line;
	private String userAgent = "Mozilla/Firefox";
	private String cookie;

	public URLGetter(String url, String cookie) {
		this.cookie = cookie;
		try {
			this.url = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String call() throws Exception {
		URLConnection conn = url.openConnection();
		conn.addRequestProperty("User-Agent", userAgent);
		conn.setRequestProperty("Cookie", cookie);
		is = conn.getInputStream();
		isr = new InputStreamReader(is);
		r = new BufferedReader(isr);
		StringBuffer sb = new StringBuffer();
		while ((line = r.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString().replaceAll("\\\\u003c", "<");
	}

}
