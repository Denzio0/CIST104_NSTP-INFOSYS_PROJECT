package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class DisputeDAO {

	public static java.util.List<String[]> getAllDisputes() {
		java.util.List<String[]> list = new java.util.ArrayList<>();

		String sql = "SELECT dispute_id, student_id, student_name, reason, program, session, "
				+ "date, status, admin_status FROM disputes ORDER BY dispute_id DESC";

		try (Connection conn = DBConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(
						new String[] { String.valueOf(rs.getInt("dispute_id")), String.valueOf(rs.getInt("student_id")),
								rs.getString("student_name"), rs.getString("reason"), rs.getString("program"),
								rs.getString("session"), rs.getString("date"), rs.getString("admin_status") });
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static java.util.List<String[]> getDisputesByStatus(String statusFilter) {
		java.util.List<String[]> list = new java.util.ArrayList<>();

		String sql = "SELECT dispute_id, student_id, student_name, reason, program, session, "
				+ "date, status, admin_status FROM disputes WHERE admin_status = ?";

		try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, statusFilter);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				list.add(
						new String[] { String.valueOf(rs.getInt("dispute_id")), String.valueOf(rs.getInt("student_id")),
								rs.getString("student_name"), rs.getString("reason"), rs.getString("severity"),
								rs.getString("officer_name"), rs.getString("date_filed"), rs.getString("status") });
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static boolean fileDispute(int studentId, String studentName, String program, String date, String session,
			String status, String reason) {
		String sql = "INSERT INTO disputes "
				+ "(student_id, student_name, program, date, session, status, reason, admin_status) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending')";

		try (java.sql.Connection conn = util.DBConnection.getConnection();
				java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

			if (conn == null)
				return false;

			ps.setInt(1, studentId);
			ps.setString(2, studentName);
			ps.setString(3, program);
			ps.setString(4, date);
			ps.setString(5, session);
			ps.setString(6, status);
			ps.setString(7, reason);

			ps.executeUpdate();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public static boolean updateDisputeStatus(int studentId, String date, String session, String status) {

        String sql =
                "UPDATE disputes " +
                "SET admin_status = ? " +
                "WHERE student_id = ? AND date = ? AND session = ?";
        
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