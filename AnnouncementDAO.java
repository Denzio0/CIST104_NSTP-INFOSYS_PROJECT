package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class AnnouncementDAO {

	public static List<String[]> getForProgram(String program) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT announcement_id, title, body, audience, priority, posted_date, is_pinned "
				+ "FROM announcements " + "WHERE audience = 'All' OR audience = ? "
				+ "ORDER BY is_pinned DESC, posted_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, program);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("announcement_id")), rs.getString("title"),
							rs.getString("body"), rs.getString("audience"), rs.getString("priority"),
							rs.getString("posted_date"), String.valueOf(rs.getInt("is_pinned")) });
				}
			}
		} catch (SQLException e) {
			System.err.println("[AnnouncementDAO] getForProgram error: " + e.getMessage());
		}
		return list;
	}

	public static int postAnnouncement(String title, String body, String audience, String priority, int postedByUserId,
			boolean pinned) {
		String sql = "INSERT INTO announcements (title, body, audience, priority, posted_by, is_pinned) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return -1;
			try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, title.trim());
				ps.setString(2, body.trim());
				ps.setString(3, audience);
				ps.setString(4, priority);
				ps.setInt(5, postedByUserId);
				ps.setInt(6, pinned ? 1 : 0);
				ps.executeUpdate();
				ResultSet keys = ps.getGeneratedKeys();
				if (keys.next())
					return keys.getInt(1);
			}
		} catch (SQLException e) {
			System.err.println("[AnnouncementDAO] postAnnouncement error: " + e.getMessage());
		}
		return -1;
	}

	public static boolean deleteAnnouncement(int announcementId) {
		String sql = "DELETE FROM announcements WHERE announcement_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, announcementId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[AnnouncementDAO] deleteAnnouncement error: " + e.getMessage());
		}
		return false;
	}

	public static void markRead(int studentId, int announcementId) {
		String sql = "INSERT IGNORE INTO read_announcements (student_id, announcement_id) VALUES (?, ?)";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ps.setInt(2, announcementId);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			System.err.println("[AnnouncementDAO] markRead error: " + e.getMessage());
		}
	}
}