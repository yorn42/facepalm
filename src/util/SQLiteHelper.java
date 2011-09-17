package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

public class SQLiteHelper {
	
	private Connection connect = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	
	public SQLiteHelper() throws ClassNotFoundException,
			SQLException {
		Class.forName("org.sqlite.JDBC");
		connect = DriverManager.getConnection("jdbc:sqlite::memory:");
		Statement statement = connect.createStatement();
		statement.executeUpdate("drop table if exists users");
		statement.executeUpdate("drop table if exists friends");
		statement.executeUpdate("drop table if exists nodes");
		statement.executeUpdate("drop table if exists edges");
	    statement.executeUpdate("create table users (id integer PRIMARY KEY AUTOINCREMENT, name string, fbid string, sex integer, single string)");
	    statement.executeUpdate("create table friends (id integer PRIMARY KEY AUTOINCREMENT, userid string, fbid string, friendfbid string)");
	    statement.executeUpdate("create table nodes (label string, url string, id string, sex varchar, single string)");
	    statement.executeUpdate("create table edges (source string, target string, weight integer, name string)");
	}
	
	public void die() {
		try {
			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createGraphDB() throws SQLException {
		String nodes = "insert into nodes(id,label,url,sex,single) select fbid,name,'http://www.facebook.com/profile.php?id='||fbid,sex,single from users;";
		String edges = "insert into edges select distinct users.fbid as source, friends.friendfbid as target, '5' as weight, 'knows' as name from users join friends where friends.userid=users.id";
		preparedStatement = connect.prepareStatement(nodes);
		preparedStatement.execute();
		preparedStatement = connect.prepareStatement(edges);
		preparedStatement.execute();
		preparedStatement.close();
	}
	
	public synchronized void insertUser(Map<String, String> details)
			throws SQLException {
		String sql = "select fbid from users where fbid=?";
		preparedStatement = connect.prepareStatement(sql);
		preparedStatement.setString(1, details.get("fbid"));
		resultSet = preparedStatement.executeQuery();
		String fbid = null;
		while (resultSet.next()) {
			fbid = resultSet.getString(1);
		}

		if (fbid != null && details.get("fbid").compareTo(fbid) == 0) {
			return;
		}

		sql = "insert into users(name, fbid, sex, single) values(?,?,?,?)";
		preparedStatement = connect.prepareStatement(sql);
		preparedStatement.setString(1, details.get("name"));
		preparedStatement.setString(2, details.get("fbid"));
		preparedStatement.setString(3, details.get("sex"));
		preparedStatement.setString(4, details.get("single"));
		preparedStatement.execute();
		preparedStatement.close();

	}
	
	public synchronized void insertFriends(ArrayList<String> friends,
			String fbid) throws SQLException {

		String sql = "select id from users where fbid=? limit 1";
		preparedStatement = connect.prepareStatement(sql);
		preparedStatement.setString(1, fbid);
		resultSet = preparedStatement.executeQuery();
		String userid = null;
		while (resultSet.next()) {
			userid = resultSet.getString(1);
			// System.out.println(userid);
		}

		for (String friend : friends) {
			sql = "insert into friends(userid,fbid,friendfbid) values(?,?,?)";
			preparedStatement = connect.prepareStatement(sql);
			preparedStatement.setString(1, userid);
			preparedStatement.setString(2, fbid);
			preparedStatement.setString(3, friend);
			preparedStatement.execute();
			preparedStatement.close();
		}

	}
	
	public Connection getConnection() {
		return this.connect;
	}

}
