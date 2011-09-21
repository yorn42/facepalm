package logic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;

import util.Config;
import util.Crawler;
import util.SQLiteHelper;
import util.URLGetter;

public class Sucker implements Callable<ArrayList<String>> {
	private Config conf = null;
	private Crawler crawl = null;
	private SQLiteHelper todb = null;
	private String fbid = null;

	public Sucker(Config conf, String fbid, Crawler crawl, SQLiteHelper todb) {
		this.conf = conf;
		this.fbid = fbid;
		this.crawl = crawl;
		this.todb = todb;
	}

	private ArrayList<String> doUser() {
		URLGetter patientZero = new URLGetter(conf.getFacebookProfileURL()
				+ this.fbid, conf.getCookies());
		String zeroPage = null;
		try {
			zeroPage = patientZero.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, String> details = crawl.getDetails(zeroPage);
		ArrayList<String> friends = crawl.getFriends(zeroPage, this.fbid);
		try {
			todb.insertUser(details);
			todb.insertFriends(friends, this.fbid);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		System.out.println("[!] Found user: " + details.get("name"));

		return friends;
	}

	@Override
	public ArrayList<String> call() throws Exception {
		return doUser();
	}
}