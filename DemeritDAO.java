package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class DemeritDAO {

	public static boolean issueDemerit(int studentId, String reason, String severity, java.util.Date demeritDate,
			String officerName, String notes) {
		String sql = "INSERT INTO demerits (student_id, reason, severity, notes, officer_name, demerit_date) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ps.setString(2, reason.trim());
				ps.setString(3, severity);
				ps.setString(4, notes != null ? notes.trim() : "");
				ps.setString(5, officerName != null ? officerName.trim() : "");
				ps.setString(6, sdf.format(demeritDate));
				ps.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			System.err.println("[DemeritDAO] issueDemerit error: " + e.getMessage());
		}
		return false;
	}

	public static List<String[]> getStudentDemerits(int studentId) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT demerit_id, reason, severity, demerit_date, "
				+ "       COALESCE(officer_name,'') AS officer_name, " + "       COALESCE(notes,'') AS notes, status "
				+ "FROM demerits WHERE student_id = ? ORDER BY demerit_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("demerit_id")), rs.getString("reason"),
							rs.getString("severity"), rs.getString("demerit_date"), rs.getString("officer_name"),
							rs.getString("notes"), rs.getString("status") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[DemeritDAO] getStudentDemerits error: " + e.getMessage());
		}
		return list;
	}

	public static List<String[]> getAllDemerits() {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT d.demerit_id, d.student_id, s.full_name, s.section, "
				+ "       d.reason, d.severity, d.demerit_date, "
				+ "       COALESCE(d.officer_name,'') AS officer_name, d.status " + "FROM demerits d "
				+ "JOIN students s ON s.student_id = d.student_id AND s.is_deleted = 0 "
				+ "ORDER BY d.demerit_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("demerit_id")),
							String.valueOf(rs.getInt("student_id")), rs.getString("full_name"), rs.getString("section"),
							rs.getString("reason"), rs.getString("severity"), rs.getString("demerit_date"),
							rs.getString("officer_name"), rs.getString("status") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[DemeritDAO] getAllDemerits error: " + e.getMessage());
		}
		return list;
	}

	public static int getTotalDemeritScore(int studentId) {
		String sql = "SELECT SUM(CASE severity " + "           WHEN 'Minor'    THEN 1 "
				+ "           WHEN 'Moderate' THEN 3 " + "           WHEN 'Major'    THEN 5 "
				+ "           ELSE 0 END) AS score " + "FROM demerits WHERE student_id = ? AND status = 'Active'";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return 0;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					return rs.getInt("score");
			}
		} catch (SQLException e) {
			System.err.println("[DemeritDAO] getTotalDemeritScore error: " + e.getMessage());
		}
		return 0;
	}

	public static int getTotalPoints(int studentId) {
		return getTotalDemeritScore(studentId);
	}

	public static int getRecordCount(int studentId) {
		String sql = "SELECT COUNT(*) FROM demerits WHERE student_id = ? AND status = 'Active'";

		try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, studentId);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt(1);
			}

		} catch (SQLException e) {
			System.err.println("[DemeritDAO] getRecordCount error: " + e.getMessage());
		}

		return 0;

	}

}