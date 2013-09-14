/*
 * Name : Shun Zhang
 * EID  : sz4554
 */
import java.sql.*;
import java.util.Date;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class Build {
	// I'm using the variable names from the tutorial.
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";
	static final String DB_URL = "jdbc:oracle:thin://localhost:1521:orcl";

	// Database credentials
	static final String USER = "system";
	static final String PASS = "oracle";

	// number of rows to insert
	static final int NUM_ROWS = 5000000;
	static final int MAX_VALUE = 100000000; // range to generate random pk. Integer.MAX_VALUE is too large.
	static final int STR_LENGTH = 10;

	public static void main(String[] args) {
		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			// make sure Driver class exists
			Class.forName(JDBC_DRIVER);

			System.out.println("Connecting to database...");
			conn= DriverManager.getConnection(DB_URL, USER, PASS);

			//createTable(conn);

			// I run these two methods separately (by commenting the other one)
			//insertionVar1(conn);
			//insertionVar2(conn);
		}
		catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}
		catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}
	}

	private static void createTable(Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;

		sql = "CREATE TABLE benchmark ("
			+"theKey int, columeA int, columeB int, filter varchar(247), PRIMARY KEY (theKey) )";
		stmt.executeUpdate(sql);

		sql = "CREATE TABLE benchmark1 ("
			+"theKey int, columeA int, columeB int, filter varchar(247), PRIMARY KEY (theKey) )";
		stmt.executeUpdate(sql);
	}

	private static void clearTable(Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;

		sql = "DELETE FROM benchmark";
		stmt.executeUpdate(sql);
		sql = "DELETE FROM benchmark1";
		stmt.executeUpdate(sql);

	}

	private static void insertionVar1(Connection conn) throws SQLException {
		System.out.println("Variation 1");
		System.out.println("Database insertion starts at " + (new Date()).toString());

		Random random = new Random();
		Statement stmt = conn.createStatement();
		String sql;

		// clear all the data, if any, exsisting in the table
		sql = "DELETE FROM benchmark";
		stmt.executeUpdate(sql);

		for (int i = 0; i < NUM_ROWS; i++) {
			sql = "INSERT INTO benchmark VALUES ("
				+ i + ", "
				+ random.nextInt(50000) + ", "
				+ random.nextInt(50000) + ", "
				+ "\'" + randString(STR_LENGTH) + "\')";
			stmt.executeUpdate(sql);

			if (i % 10000 == 0) {
				System.out.println(i + " rows added.");
			}
		}

		System.out.println("Database insertion ends at " + (new Date()).toString());
	}
	
	private static void insertionVar2(Connection conn) throws SQLException {
		System.out.println("Variation 2");
		System.out.println("Database insertion starts at " + (new Date()).toString());

		Random random = new Random();
		Statement stmt = conn.createStatement();
		String sql;

		// clear all the data, if any, in the table
		sql = "DELETE FROM benchmark1";
		stmt.executeUpdate(sql);

		// pk is used, if pkList[pk] == true
		boolean[] pkList = new boolean[MAX_VALUE];
		int pk;

		for (int i = 0; i < NUM_ROWS; i++) {
			// make sure pk won't repeat
			do {
				pk = random.nextInt(MAX_VALUE);
			} while (pkList[pk]);

			sql = "INSERT INTO benchmark1 VALUES ("
				+ pk + ", "
				+ random.nextInt(50000) + ", "
				+ random.nextInt(50000) + ", "
				+ "\'" + randString(STR_LENGTH) + "\')";
			stmt.executeUpdate(sql);

			if (i % 10000 == 0) {
				System.out.println(i + " rows added.");
			}

			pkList[pk] = true;
		}

		System.out.println("Database insertion ends at " + (new Date()).toString());
	}

	private static String randString(int length) {
		char[] result = new char[length];
		Random random = new Random();

		for (int i = 0; i < length; i++) {
			result[i] = (char)(97 + random.nextInt(26)); // make sure it's a lowerclass letter
		}

		return new String(result);
	}
}
