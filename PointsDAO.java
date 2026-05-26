package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class PointsDAO {

	public static boolean addTransaction(int studentId, int amount, String type, String reason,
			java.util.Date transactionDate, int adminId) {
		String sql = "INSERT INTO points (student_id, amount, type, reason, transaction_date, admin_id) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ps.setInt(2, amount);
				ps.setString(3, type);
				ps.setString(4, reason.trim());
				ps.setString(5, sdf.format(transactionDate));
				ps.setInt(6, adminId);
				ps.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			System.err.println("[PointsDAO] addTransaction error: " + e.getMessage());
		}
		return false;
	}

	public static int getBalance(int studentId) {
		String sql = "SELECT COALESCE(SUM(CASE WHEN type='award' THEN amount ELSE -amount END),0) AS bal "
				+ "FROM points WHERE student_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return 0;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					return rs.getInt("bal");
			}
		} catch (SQLException e) {
			System.err.println("[PointsDAO] getBalance error: " + e.getMessage());
		}
		return 0;
	}

	public static List<String[]> getStudentTransactions(int studentId) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT transaction_id, amount, type, reason, transaction_date "
				+ "FROM points WHERE student_id = ? ORDER BY transaction_date DESC";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("transaction_id")),
							String.valueOf(rs.getInt("amount")), rs.getString("type"), rs.getString("reason"),
							rs.getString("transaction_date") });
				}
			}
		} catch (SQLException e) {
			System.err.println("[PointsDAO] getStudentTransactions error: " + e.getMessage());
		}
		return list;
	}

	public static List<String[]> getLeaderboard(String program, int limit) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT p.student_id, s.full_name, s.section, s.program, "
				+ "       COALESCE(SUM(CASE WHEN p.type='award' THEN p.amount ELSE -p.amount END),0) AS balance "
				+ "FROM points p " + "JOIN students s ON s.student_id = p.student_id AND s.is_deleted = 0 "
				+ ("All".equals(program) ? "" : "WHERE s.program = ? ")
				+ "GROUP BY p.student_id, s.full_name, s.section, s.program " + "ORDER BY balance DESC " + "LIMIT ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			PreparedStatement ps = conn.prepareStatement(sql);
			if ("All".equals(program)) {
				ps.setInt(1, limit);
			} else {
				ps.setString(1, program);
				ps.setInt(2, limit);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new String[] { String.valueOf(rs.getInt("student_id")), rs.getString("full_name"),
						rs.getString("section"), rs.getString("program"), String.valueOf(rs.getInt("balance")) });
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.err.println("[PointsDAO] getLeaderboard error: " + e.getMessage());
		}
		return list;
	}
}