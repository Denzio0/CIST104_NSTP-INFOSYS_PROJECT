package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class PerformanceDAO {

	public static boolean addPerformance(int studentId, String drillName, java.util.Date drillDate, String rating,
			String remarks, String officerName) {
		String sql = "INSERT INTO performance (student_id, drill_name, drill_date, rating, remarks, officer_name) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ps.setString(2, drillName.trim());
				ps.setString(3, sdf.format(drillDate));
				ps.setString(4, rating);
				ps.setString(5, remarks != null ? remarks.trim() : "");
				ps.setString(6, officerName != null ? officerName.trim() : "");
				ps.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			System.err.println("[PerformanceDAO] addPerformance error: " + e.getMessage());
		}
		return false;
	}

	public static List<String[]> getStudentPerformance(int studentId) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT entry_id, drill_name, drill_date, rating, " + "       COALESCE(remarks,'') AS remarks, "
				+ "       COALESCE(officer_name,'') AS officer_name "
				+ "FROM performance WHERE student_id = ? ORDER BY drill_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("entry_id")), rs.getString("drill_name"),
							rs.getString("drill_date"), rs.getString("rating"), rs.getString("remarks"),
							rs.getString("officer_name") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[PerformanceDAO] getStudentPerformance error: " + e.getMessage());
		}
		return list;
	}

	public static boolean checkAtRiskFlag(int studentId) {
		String sql = "SELECT rating FROM performance WHERE student_id = ? " + "ORDER BY drill_date DESC LIMIT 3";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				int badCount = 0;
				while (rs.next()) {
					String r = rs.getString("rating");
					if ("Needs_Improvement".equals(r) || "Unsatisfactory".equals(r))
						badCount++;
				}
				return badCount >= 3;
			}
		} catch (SQLException e) {
			System.err.println("[PerformanceDAO] checkAtRiskFlag error: " + e.getMessage());
		}
		return false;
	}

	public static List<String[]> getAllPerformance() {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT p.entry_id, p.student_id, s.full_name, s.section, "
				+ "       p.drill_name, p.drill_date, p.rating, " + "       COALESCE(p.remarks,'') AS remarks, "
				+ "       COALESCE(p.officer_name,'') AS officer_name " + "FROM performance p "
				+ "JOIN students s ON s.student_id = p.student_id AND s.is_deleted = 0 " + "ORDER BY p.drill_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("entry_id")),
							String.valueOf(rs.getInt("student_id")), rs.getString("full_name"), rs.getString("section"),
							rs.getString("drill_name"), rs.getString("drill_date"), rs.getString("rating"),
							rs.getString("remarks"), rs.getString("officer_name") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[PerformanceDAO] getAllPerformance error: " + e.getMessage());
		}
		return list;
	}

	public static java.util.List<String[]> getPerformanceForStudent(String studentIdStr) {
		int studentId = Integer.parseInt(studentIdStr);
		java.util.List<String[]> list = new java.util.ArrayList<>();

		String sql = "SELECT entry_id, drill_name, drill_date, rating, remarks, officer_name "
				+ "FROM performance WHERE student_id = ? ORDER BY drill_date DESC";

		try (java.sql.Connection conn = util.DBConnection.getConnection();
				java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

			if (conn == null)
				return list;

			ps.setInt(1, studentId);
			java.sql.ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				list.add(new String[] { String.valueOf(rs.getInt("entry_id")), rs.getString("drill_name"),
						rs.getString("drill_date"), rs.getString("rating"), rs.getString("remarks"),
						rs.getString("officer_name") });
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}
}