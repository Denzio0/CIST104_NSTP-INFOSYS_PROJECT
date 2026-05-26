package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class CalendarDAO {

	public static List<String[]> getEventsForProgram(String program) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT event_id, title, event_date, program_type, " + "       COALESCE(location,'') AS location, "
				+ "       COALESCE(description,'') AS description, reminder_flag " + "FROM calendar_events "
				+ "WHERE program_type = 'All' OR program_type = ? " + "ORDER BY event_date ASC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, program);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("event_id")), rs.getString("title"),
							rs.getString("event_date"), rs.getString("program_type"), rs.getString("location"),
							rs.getString("description"), String.valueOf(rs.getInt("reminder_flag")) });
				}
			}
		} catch (SQLException e) {
			System.err.println("[CalendarDAO] getEventsForProgram error: " + e.getMessage());
		}
		return list;
	}

	public static int addEvent(String title, java.util.Date eventDate, String programType, String location,
			String description, int createdBy) {
		String sql = "INSERT INTO calendar_events "
				+ "(title, event_date, program_type, location, description, created_by) " + "VALUES (?, ?, ?, ?, ?, ?)";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return -1;
			try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, title.trim());
				ps.setString(2, sdf.format(eventDate));
				ps.setString(3, programType);
				ps.setString(4, location != null ? location.trim() : "");
				ps.setString(5, description != null ? description.trim() : "");
				ps.setInt(6, createdBy);
				ps.executeUpdate();
				ResultSet keys = ps.getGeneratedKeys();
				if (keys.next())
					return keys.getInt(1);
			}
		} catch (SQLException e) {
			System.err.println("[CalendarDAO] addEvent error: " + e.getMessage());
		}
		return -1;
	}

	public static boolean deleteEvent(int eventId) {
		String sql = "DELETE FROM calendar_events WHERE event_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, eventId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[CalendarDAO] deleteEvent error: " + e.getMessage());
		}
		return false;
	}
}
