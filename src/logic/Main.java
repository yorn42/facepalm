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

		// Configuration of succulent
		try {
			conf = new Config();
			conf.doConfig(args[1]);
			crawl = new Crawler(conf);
			todb = new SQLiteHelper();
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

		// GO!
		ExecutorService executor = Executors.newFixedThreadPool(15);

		// Get Patient Zero
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

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

		// Write Graph
		String graph = null;
		try {

			todb.createGraphDB();
			GraphCreator creator = new GraphCreator(todb);
			System.out.println("[!] Calculating graph... ");
			graph = creator.createGraphFromSQL();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("[!] Writing gexf file... ");
		new WriteToFile().writeToFile(graph, conf.getGexfPath() + "/" + args[0]);
		
		System.out.println("[!] Check " + conf.getGexfPath() + args[0] + "/index.html for results!");
		
		executor.shutdown();
		todb.die();
	}
}