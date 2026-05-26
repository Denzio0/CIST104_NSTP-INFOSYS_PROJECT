package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

	private static final String DB_URL = "jdbc:mysql://localhost:3306/nstp_infosystem"
			+ "?useSSL=false&serverTimezone=Asia/Manila" + "&allowPublicKeyRetrieval=true"
			+ "&useUnicode=true&characterEncoding=utf8";
	private static final String DB_USER = "root";
	private static final String DB_PASS = "";

	static {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {

			try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException ex) {
				System.err.println("[DBConnection] MySQL driver not found. Add mysql-connector-java to classpath.");
			}
		}
	}

	public static Connection getConnection() {
		try {
			return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
		} catch (SQLException e) {
			System.err.println("[DBConnection] Cannot connect to database: " + e.getMessage());
			return null;
		}
	}

	public static void testConnection() {
		try (Connection c = getConnection()) {
			if (c != null && !c.isClosed()) {
				System.out.println("[DBConnection] Connection OK — database: nstp_infosystem");
			} else {
				System.err.println("[DBConnection] Connection FAILED — check DB_URL / credentials");
			}
		} catch (SQLException e) {
			System.err.println("[DBConnection] testConnection error: " + e.getMessage());
		}
	}
}