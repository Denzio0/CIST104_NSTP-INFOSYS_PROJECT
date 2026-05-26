package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class EngagementDAO {

	public static int addEngagement(int studentId, String activityName, java.util.Date activityDate, String category,
			double hoursRendered) {
		String sql = "INSERT INTO engagement (student_id, activity_name, activity_date, "
				+ "                        category, hours_rendered, verified) " + "VALUES (?, ?, ?, ?, ?, 0)";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return -1;
			try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, studentId);
				ps.setString(2, activityName.trim());
				ps.setString(3, sdf.format(activityDate));
				ps.setString(4, category != null ? category.trim() : "General");
				ps.setDouble(5, hoursRendered);
				ps.executeUpdate();
				ResultSet keys = ps.getGeneratedKeys();
				if (keys.next())
					return keys.getInt(1);
			}
		} catch (SQLException e) {
			System.err.println("[EngagementDAO] addEngagement error: " + e.getMessage());
		}
		return -1;
	}

	public static List<String[]> getStudentEngagement(int studentId) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT activity_id, activity_name, activity_date, "
				+ "       COALESCE(category,'') AS category, hours_rendered, verified "
				+ "FROM engagement WHERE student_id = ? ORDER BY activity_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("activity_id")), rs.getString("activity_name"),
							rs.getString("activity_date"), rs.getString("category"),
							String.valueOf(rs.getDouble("hours_rendered")), String.valueOf(rs.getInt("verified")) });
				}
			}
		} catch (SQLException e) {
			System.err.println("[EngagementDAO] getStudentEngagement error: " + e.getMessage());
		}
		return list;
	}

	public static List<String[]> getAllEngagement() {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT e.activity_id, e.student_id, s.full_name, s.section, "
				+ "       e.activity_name, e.activity_date, "
				+ "       COALESCE(e.category,'') AS category, e.hours_rendered, e.verified " + "FROM engagement e "
				+ "JOIN students s ON s.student_id = e.student_id AND s.is_deleted = 0 "
				+ "ORDER BY e.activity_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("activity_id")),
							String.valueOf(rs.getInt("student_id")), rs.getString("full_name"), rs.getString("section"),
							rs.getString("activity_name"), rs.getString("activity_date"), rs.getString("category"),
							String.valueOf(rs.getDouble("hours_rendered")), String.valueOf(rs.getInt("verified")) });
				}
			}
		} catch (SQLException e) {
			System.err.println("[EngagementDAO] getAllEngagement error: " + e.getMessage());
		}
		return list;
	}

	public static double getTotalVerifiedHours(int studentId) {
		String sql = "SELECT COALESCE(SUM(hours_rendered),0) AS total "
				+ "FROM engagement WHERE student_id = ? AND verified = 1";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return 0.0;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					return rs.getDouble("total");
			}
		} catch (SQLException e) {
			System.err.println("[EngagementDAO] getTotalVerifiedHours error: " + e.getMessage());
		}
		return 0.0;
	}

	public static double getTotalHours(int studentId) {
		String sql = "SELECT COALESCE(SUM(hours_rendered),0) AS total " + "FROM engagement WHERE student_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return 0.0;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					return rs.getDouble("total");
			}
		} catch (SQLException e) {
			System.err.println("[EngagementDAO] getTotalHours error: " + e.getMessage());
		}
		return 0.0;
	}

	public static boolean verifyEngagement(int activityId) {
		String sql = "UPDATE engagement SET verified = 1 WHERE activity_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, activityId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[EngagementDAO] verifyEngagement error: " + e.getMessage());
		}
		return false;
	}

	public static double getTotalHours1(int studentId) {

		String sql =

				"SELECT COALESCE(SUM(hours_rendered),0) " + "FROM cwts_engagement " + "WHERE student_id = ?";

		try (

				java.sql.Connection conn = util.DBConnection.getConnection();

				java.sql.PreparedStatement ps = conn.prepareStatement(sql)

		) {

			ps.setInt(1, studentId);

			java.sql.ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getDouble(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	public static java.util.List<String[]> getStudentEngagementLogs(int studentId) {

		java.util.List<String[]> list = new java.util.ArrayList<>();

		String sql = "SELECT engagement_date, activity, location, hours, points " + "FROM cwts_engagement "
				+ "WHERE student_id = ? " + "ORDER BY engagement_date DESC";

		try (

				java.sql.Connection conn = util.DBConnection.getConnection();

				java.sql.PreparedStatement ps = conn.prepareStatement(sql)

		) {

			ps.setInt(1, studentId);

			java.sql.ResultSet rs = ps.executeQuery();

			while (rs.next()) {

				list.add(new String[] {

						rs.getString("engagement_date"), rs.getString("activity"), rs.getString("location"),
						rs.getString("hours"), rs.getString("points")

				});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}
}