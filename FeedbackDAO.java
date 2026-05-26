package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class FeedbackDAO {

	public static boolean submitFeedback(int studentId, String type, String subject, String body) {
		String sql = "INSERT INTO feedback (student_id, type, subject, body) VALUES (?, ?, ?, ?)";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ps.setString(2, type);
				ps.setString(3, subject.trim());
				ps.setString(4, body.trim());
				ps.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			System.err.println("[FeedbackDAO] submitFeedback error: " + e.getMessage());
		}
		return false;
	}

	public static List<String[]> getAllFeedback() {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT f.feedback_id, f.student_id, s.full_name, s.program, "
				+ "       f.submitted_date, f.type, f.subject, f.body, f.status, "
				+ "       COALESCE(f.admin_response,'') AS admin_response " + "FROM feedback f "
				+ "JOIN students s ON s.student_id = f.student_id " + "ORDER BY f.submitted_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("feedback_id")),
							String.valueOf(rs.getInt("student_id")), rs.getString("full_name"), rs.getString("program"),
							rs.getString("submitted_date"), rs.getString("type"), rs.getString("subject"),
							rs.getString("body"), rs.getString("status"), rs.getString("admin_response") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[FeedbackDAO] getAllFeedback error: " + e.getMessage());
		}
		return list;
	}

	public static boolean respondToFeedback(int feedbackId, String adminResponse) {
		String sql = "UPDATE feedback SET status='Resolved', admin_response=? WHERE feedback_id=?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, adminResponse.trim());
				ps.setInt(2, feedbackId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[FeedbackDAO] respondToFeedback error: " + e.getMessage());
		}
		return false;
	}
}