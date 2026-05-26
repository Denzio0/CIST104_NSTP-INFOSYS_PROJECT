package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class AttendanceDAO {

	public static int saveSession(String sessionName, java.util.Date sessionDate, List<int[]> studentIds,
			List<String> statuses, int loggedByUserId) {
		String sql = "INSERT INTO attendance " + "(student_id, session_name, session_date, status, remarks, logged_by) "
				+ "VALUES (?, ?, ?, ?, '', ?) "
				+ "ON DUPLICATE KEY UPDATE status = VALUES(status), logged_by = VALUES(logged_by)";
		int count = 0;
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		String dateStr = sdf.format(sessionDate);
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return 0;
			conn.setAutoCommit(false);
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				for (int i = 0; i < studentIds.size(); i++) {
					ps.setInt(1, studentIds.get(i)[0]);
					ps.setString(2, sessionName);
					ps.setString(3, dateStr);
					ps.setString(4, statuses.get(i));
					ps.setInt(5, loggedByUserId);
					ps.addBatch();
				}
				int[] rows = ps.executeBatch();
				for (int r : rows)
					count += (r > 0 ? 1 : 0);
				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
				System.err.println("[AttendanceDAO] saveSession batch error: " + e.getMessage());
			}
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			System.err.println("[AttendanceDAO] saveSession connection error: " + e.getMessage());
		}
		return count;
	}

	public static List<String[]> getStudentAttendance(int studentId) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT a.record_id, a.session_name, a.session_date, a.status, "
				+ "       COALESCE(d.status,'') AS dispute_status " + "FROM attendance a "
				+ "LEFT JOIN disputes d ON d.record_id = a.record_id AND d.student_id = a.student_id "
				+ "WHERE a.student_id = ? AND a.is_deleted = 0 " + "ORDER BY a.session_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("record_id")), rs.getString("session_name"),
							rs.getString("session_date"), rs.getString("status"), rs.getString("dispute_status") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[AttendanceDAO] getStudentAttendance error: " + e.getMessage());
		}
		return list;
	}

	public static double computeAttendanceRate(int studentId) {
		String sql = "SELECT COUNT(*) AS total, "
				+ "       SUM(CASE WHEN status IN ('Present','Excused') THEN 1 ELSE 0 END) AS present_count "
				+ "FROM attendance WHERE student_id = ? AND is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return 0.0;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					int total = rs.getInt("total");
					if (total == 0)
						return 0.0;
					return rs.getDouble("present_count") * 100.0 / total;
				}
			}
		} catch (SQLException e) {
			System.err.println("[AttendanceDAO] computeAttendanceRate error: " + e.getMessage());
		}
		return 0.0;
	}

	public static int[] getAttendanceSummary(int studentId) {
		String sql = "SELECT COUNT(*) AS total, "
				+ "       SUM(CASE WHEN status IN ('Present','Excused') THEN 1 ELSE 0 END) AS present_count "
				+ "FROM attendance WHERE student_id = ? AND is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return new int[] { 0, 0 };
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					return new int[] { rs.getInt("total"), rs.getInt("present_count") };
				}
			}
		} catch (SQLException e) {
			System.err.println("[AttendanceDAO] getAttendanceSummary error: " + e.getMessage());
		}
		return new int[] { 0, 0 };
	}

	public static List<String[]> getAllDisputes(String statusFilter) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT d.dispute_id, d.student_id, d.record_id, d.reason, "
				+ "       d.requested_status, d.submitted_date, d.status, d.admin_response, "
				+ "       s.full_name, a.session_name, a.session_date, a.status AS orig_status, " + "       st.program "
				+ "FROM disputes d " + "JOIN students st ON st.student_id = d.student_id "
				+ "JOIN attendance a ON a.record_id = d.record_id " + "JOIN students s ON s.student_id = d.student_id "
				+ "WHERE d.status = ? " + "ORDER BY d.submitted_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, statusFilter);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("dispute_id")),
							String.valueOf(rs.getInt("student_id")), String.valueOf(rs.getInt("record_id")),
							rs.getString("reason"), rs.getString("requested_status"), rs.getString("submitted_date"),
							rs.getString("status"),
							rs.getString("admin_response") != null ? rs.getString("admin_response") : "",
							rs.getString("full_name"), rs.getString("session_name"), rs.getString("session_date"),
							rs.getString("orig_status"), rs.getString("program") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[AttendanceDAO] getAllDisputes error: " + e.getMessage());
		}
		return list;
	}

	public static boolean fileDispute(int studentId, int recordId, String reason, String requestedStatus) {
		String sql = "INSERT INTO disputes (student_id, record_id, reason, requested_status, submitted_date) "
				+ "VALUES (?, ?, ?, ?, CURDATE())";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ps.setInt(2, recordId);
				ps.setString(3, reason);
				ps.setString(4, requestedStatus);
				ps.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			System.err.println("[AttendanceDAO] fileDispute error: " + e.getMessage());
		}
		return false;
	}

	public static boolean resolveDispute(int disputeId, String newStatus, String adminResponse, int recordId,
			String approvedAttendanceStatus) {
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			conn.setAutoCommit(false);
			try {
				String updDisp = "UPDATE disputes SET status = ?, admin_response = ? WHERE dispute_id = ?";
				try (PreparedStatement ps = conn.prepareStatement(updDisp)) {
					ps.setString(1, newStatus);
					ps.setString(2, adminResponse);
					ps.setInt(3, disputeId);
					ps.executeUpdate();
				}
				if ("Approved".equals(newStatus) && approvedAttendanceStatus != null) {
					String updAtt = "UPDATE attendance SET status = ? WHERE record_id = ?";
					try (PreparedStatement ps = conn.prepareStatement(updAtt)) {
						ps.setString(1, approvedAttendanceStatus);
						ps.setInt(2, recordId);
						ps.executeUpdate();
					}
				}
				conn.commit();
				return true;
			} catch (SQLException e) {
				conn.rollback();
				System.err.println("[AttendanceDAO] resolveDispute error: " + e.getMessage());
			}
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			System.err.println("[AttendanceDAO] resolveDispute connection error: " + e.getMessage());
		}
		return false;
	}

	public static java.util.List<String[]> getAttendanceByStudentId(int studentId) {

		java.util.List<String[]> list = new java.util.ArrayList<>();

		String sql = "SELECT record_id, student_id, session_date, " + "session_name, status " + "FROM attendance "
				+ "WHERE student_id = ? AND is_deleted = 0";

		try (java.sql.Connection conn = util.DBConnection.getConnection();

				java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, studentId);

			java.sql.ResultSet rs = ps.executeQuery();

			while (rs.next()) {

				list.add(new String[] {

						String.valueOf(rs.getInt("record_id")),

						String.valueOf(rs.getInt("student_id")),

						rs.getString("session_date"),

						rs.getString("session_name"),

						rs.getString("status") });
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static java.util.List<String[]> getAttendanceWithDisputes(int studentId) {

		java.util.List<String[]> list = new java.util.ArrayList<>();

		String sql =

				"SELECT " + "a.session_date, " + "a.session_name, " + "a.status, "
						+ "COALESCE(d.status, '') AS dispute_status " + "FROM attendance a " +

						"LEFT JOIN disputes d " + "ON a.record_id = d.record_id AND d.student_id = a.student_id " +

						"WHERE a.student_id = ? AND a.is_deleted = 0 " + "ORDER BY a.session_date DESC";

		try (

				java.sql.Connection conn = util.DBConnection.getConnection();

				java.sql.PreparedStatement ps = conn.prepareStatement(sql)

		) {

			ps.setInt(1, studentId);

			java.sql.ResultSet rs = ps.executeQuery();

			while (rs.next()) {

				list.add(new String[] {

						rs.getString("session_date"), rs.getString("session_name"), rs.getString("status"),
						rs.getString("dispute_status")
				});

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

		return list;
	}

	public static int getPresentAttendanceCount(int studentId) {

		String sql =

				"SELECT COUNT(*) FROM attendance " + "WHERE student_id = ? " + "AND (status='Present' "
						+ "OR status='Excused')";

		try (

				java.sql.Connection conn = util.DBConnection.getConnection();

				java.sql.PreparedStatement ps = conn.prepareStatement(sql)

		) {

			ps.setInt(1, studentId);

			java.sql.ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	public static int getAttendanceCount(int studentId) {

		String sql = "SELECT COUNT(*) FROM attendance " + "WHERE student_id = ?";

		try (

				java.sql.Connection conn = util.DBConnection.getConnection();

				java.sql.PreparedStatement ps = conn.prepareStatement(sql)

		) {

			ps.setInt(1, studentId);

			java.sql.ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	public static boolean saveAttendance(int studentId, String program, String dateStr, String sessionName,
			String status) {

		String sql = "INSERT INTO attendance (student_id, session_name, session_date, status, remarks, logged_by) "
				+ "VALUES (?, ?, ?, ?, '', NULL) "
				+ "ON DUPLICATE KEY UPDATE status = VALUES(status)";

		try (java.sql.Connection conn = util.DBConnection.getConnection();
				java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

			if (conn == null)
				return false;

			java.text.SimpleDateFormat inFmt = new java.text.SimpleDateFormat("MMM dd, yyyy");
			java.text.SimpleDateFormat outFmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
			String sqlDate;
			try {
				sqlDate = outFmt.format(inFmt.parse(dateStr));
			} catch (Exception ex) {
				sqlDate = dateStr;
			}

			ps.setInt(1, studentId);
			ps.setString(2, sessionName);
			ps.setString(3, sqlDate);
			ps.setString(4, status);

			ps.executeUpdate();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
	public static boolean updateAttendanceStatus(int studentId, String date, String session, String status) {

		String sql = "UPDATE attendance " + "SET status = ? "
				+ "WHERE student_id = ? AND session_date = ? AND session_name = ?";

		try (java.sql.Connection conn = util.DBConnection.getConnection();
				java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

			if (conn == null)
				return false;

			ps.setString(1, status);
			ps.setInt(2, studentId);
			ps.setString(3, date);
			ps.setString(4, session);

			return ps.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}