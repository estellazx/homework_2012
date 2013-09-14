import java.sql.*;
import java.util.Date;
import java.util.Random;

public class Lab7 {
	// I'm using the variable names from the tutorial.
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";
	static final String DB_URL = "jdbc:oracle:thin://localhost:1521:orcl";

	// Database credentials
	static final String USER = "system";
	static final String PASS = "oracle";

	// number of rows to insert
	static Connection conn = null;

	static final int[] queries = {2395, 8294, 12032, 16930, 21847, 28493, 32583, 37294, 40483, 48453};

	public static void main(String[] args) {
		Statement stmt = null;
		String sql = null;

		try {
			// make sure Driver class exists
			Class.forName(JDBC_DRIVER);

			System.out.println("Connecting to database...");
			conn= DriverManager.getConnection(DB_URL, USER, PASS);

			String[] tableNames = {"benchmark", "benchmark1"};

			for (String name : tableNames) {
				System.out.println("Working on table " + name);

				System.out.println("Physical Organization 1");
				query(name);

				System.out.println("Physical Organization 2");
				createIndex(name, 1);
				query(name);
				clearIndex(name, 1);

				System.out.println("Physical Organization 3");
				createIndex(name, 2);
				query(name);
				clearIndex(name, 2);

				System.out.println("Physical Organization 4");
				createIndex(name, 1);
				createIndex(name, 2);
				query(name);
				clearIndex(name, 1);
				clearIndex(name, 2);
			}
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

	private static void clearIndex(String name, int colume) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;

		if (colume == 1) {
			sql = "DROP INDEX aindex";
			System.out.println(sql);
		}
		else {
			sql = "DROP INDEX bindex";
			System.out.println(sql);
		}
		stmt.executeUpdate(sql);
	}

	private static void createIndex(String name, int colume) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		Date start, end;

		if (colume == 1) {
			sql = "CREATE INDEX aindex on " + name + " (columeA)";
			System.out.println(sql);
		}
		else {
			sql = "CREATE INDEX bindex on " + name + " (columeB)";
			System.out.println(sql);
		}

		start = new Date();
		stmt.executeUpdate(sql);
		end = new Date();

		System.out.println("Duration " + (end.getTime() - start.getTime()));
	}

	private static void query(String name) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		int i;
		Date start, end;

		System.out.println("Load");

		start = new Date();
		for (i = 0; i < 10; i++) {
			sql = "SELECT * FROM " + name;
			System.out.println(sql);
			stmt.executeUpdate(sql);
		}
		end = new Date();
		System.out.println("Duration " + (end.getTime() - start.getTime()));

		System.out.println("Query 1");

		start = new Date();
		for (i = 0; i < 10; i++) {
			sql = "SELECT * FROM " + name + " WHERE columeA=" + queries[i];
			System.out.println(sql);
			stmt.executeUpdate(sql);
		}
		end = new Date();
		System.out.println("Duration " + (end.getTime() - start.getTime()));
		
		System.out.println("Query 2");

		start = new Date();
		for (i = 0; i < 10; i++) {
			sql = "SELECT * FROM " + name + " WHERE columeB=" + queries[i];
			System.out.println(sql);
			stmt.executeUpdate(sql);
		}
		end = new Date();
		System.out.println("Duration " + (end.getTime() - start.getTime()));

		System.out.println("Query 3");

		start = new Date();
		for (i = 0; i < 10; i++) {
			sql = "SELECT * FROM " + name + " WHERE columeA=" + queries[i] + " AND columeB=" + queries[i];
			System.out.println(sql);
			stmt.executeUpdate(sql);
		}
		end = new Date();
		System.out.println("Duration " + (end.getTime() - start.getTime()));
	}
}
