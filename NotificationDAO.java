package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class NotificationDAO {

	public static List<String[]> getUnread(int studentId) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT notif_id, type, message, created_date "
				+ "FROM notifications WHERE student_id = ? AND is_read = 0 " + "ORDER BY created_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("notif_id")), rs.getString("type"),
							rs.getString("message"), rs.getString("created_date") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[NotificationDAO] getUnread error: " + e.getMessage());
		}
		return list;
	}

	public static void markRead(int notifId) {
		String sql = "UPDATE notifications SET is_read = 1 WHERE notif_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, notifId);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			System.err.println("[NotificationDAO] markRead error: " + e.getMessage());
		}
	}

	public static void insertIfNew(int studentId, String type, String message) {
		String sql = "INSERT IGNORE INTO notifications (student_id, type, message) VALUES (?, ?, ?)";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ps.setString(2, type);
				ps.setString(3, message);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			System.err.println("[NotificationDAO] insertIfNew error: " + e.getMessage());
		}
	}

	public static int countUnread(int studentId) {
		String sql = "SELECT COUNT(*) FROM notifications WHERE student_id = ? AND is_read = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return 0;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					return rs.getInt(1);
			}
		} catch (SQLException e) {
			System.err.println("[NotificationDAO] countUnread error: " + e.getMessage());
		}
		return 0;
	}
}