package logic;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import util.Config;
import util.Crawler;
import util.GraphCreator;
import util.SQLiteHelper;
import util.PartitionCreator;
import util.WriteToFile;

public class Main {
	private static Config conf = null;
	private static SQLiteHelper todb = null;
	private static Crawler crawl = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length < 2) {
			System.out
					.println("[X] Usage: succulent <FACEBOOKID> <PATH TO CONFIG FILE>");
			System.exit(1);
		}
		WriteToFile wtf = new WriteToFile();

		// configuration of facepalm
		try {
			conf = new Config();
			conf.doConfig(args[1]);
			crawl = new Crawler(conf);
			todb = new SQLiteHelper();
			
			// Write first stats to file.
			wtf.setupFiles(conf.getGexfPath() + "/" + args[0]);
			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put("<a href=\"http://www.facebook.com/profile.php?id="
					+ args[0] + "\">FBID:</a>", args[0].toString());
			WriteToFile
					.writeStatsToFile(hm, conf.getGexfPath() + "/" + args[0]);
		} catch (IOException e) {
			System.out.println("[X] Configuration error! Is the config file '"
					+ args[1] + "' there?");
			System.exit(1);
		} catch (Exception ex) {
			System.out.println("[X] Unknown error: ");
			ex.printStackTrace();
			System.exit(1);
		}

		if (conf == null) {
			System.out
					.println("[X] Something went wrong during configuration!");
			System.exit(1);
		}

		// thread pool, 15 is a good choice.
		ExecutorService executor = Executors.newFixedThreadPool(15);

		// get Patient Zero
		Future<ArrayList<String>> firstFriends = executor.submit(new Sucker(
				conf, args[0], crawl, todb));
		
		// used by the threads, this is the patient zero friend list
		Map<String, Future<ArrayList<String>>> parallel = new HashMap<String, Future<ArrayList<String>>>();

		// get the friends and convert them to users
		try {
			for (String friend : firstFriends.get()) {
				parallel.put(friend,
						executor.submit(new Sucker(conf, friend, crawl, todb)));
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			e1.printStackTrace();
		}

		// get the friends of the friends, not making them users
		for (Map.Entry<String, Future<ArrayList<String>>> entry : parallel
				.entrySet()) {
			try {
				// not so nice ...
				String fbid = entry.getKey();
				ArrayList<String> friends = entry.getValue().get();
				todb.insertFriends(friends, fbid);
				// System.out.println(fbid);

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		// calculate graph
		String graph = null;
		try {

			todb.createGraphDB();
			GraphCreator creator = new GraphCreator(todb, conf.getGexfPath()
					+ "/" + args[0]);
			System.out.println("[!] Calculating graph... ");
			graph = creator.createGraphFromSQL();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// shut everything down
		executor.shutdown();
		todb.die();

		// write to files
		System.out.println("[!] Writing gexfs... ");
		wtf.writeGexf(graph, conf.getGexfPath() + "/" + args[0]);
		PartitionCreator sc = new PartitionCreator();
		String parts = sc.createPartitions(conf.getGexfPath() + "/" + args[0]);
		wtf.writeParts(parts, conf.getGexfPath() + "/" + args[0]);
		wtf.finalizeFiles(conf.getGexfPath() + "/" + args[0]);
		System.out.println("[!] Check " + conf.getGexfPath() + "/" + args[0]
				+ "/index.html for results!");
	}
}