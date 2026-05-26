package dao;

import util.DBConnection;
import java.sql.*;
import java.util.*;

public class StudentDAO {

	public static List<String[]> getAllStudents(String programFilter) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT student_id, full_name, program, year_level, section, "
				+ "       COALESCE(contact_no,'') AS contact_no, " + "       COALESCE(email,'') AS email "
				+ "FROM students " + "WHERE is_deleted = 0 "
				+ (("ROTC".equals(programFilter) || "CWTS".equals(programFilter)) ? "AND program = ? " : "")
				+ "ORDER BY full_name";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			PreparedStatement ps = conn.prepareStatement(sql);
			if ("ROTC".equals(programFilter) || "CWTS".equals(programFilter))
				ps.setString(1, programFilter);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new String[] { String.valueOf(rs.getInt("student_id")), rs.getString("full_name"),
						rs.getString("program"), rs.getString("year_level"), rs.getString("section"),
						rs.getString("contact_no"), rs.getString("email") });
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.err.println("[StudentDAO] getAllStudents error: " + e.getMessage());
		}
		return list;

	}

	public static String[] getStudentById(int studentId) {
		String sql = "SELECT student_id, full_name, program, year_level, section, "
				+ "       COALESCE(contact_no,'') AS contact_no, " + "       COALESCE(email,'') AS email "
				+ "FROM students WHERE student_id = ? AND is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return null;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					return new String[] { String.valueOf(rs.getInt("student_id")), rs.getString("full_name"),
							rs.getString("program"), rs.getString("year_level"), rs.getString("section"),
							rs.getString("contact_no"), rs.getString("email") };
				}
			}
		} catch (SQLException e) {
			System.err.println("[StudentDAO] getStudentById error: " + e.getMessage());
		}
		return null;
	}

	public static int createStudent(String fullName, String program, String yearLevel, String section, String contactNo,
			String email) {
		String sql = "INSERT INTO students (full_name, program, year_level, section, contact_no, email) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return -1;
			try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, fullName.trim());
				ps.setString(2, program);
				ps.setString(3, yearLevel.trim().isEmpty() ? "1st Year" : yearLevel.trim());
				ps.setString(4, section.trim());
				ps.setString(5, contactNo.trim().isEmpty() ? null : contactNo.trim());
				ps.setString(6, email.trim().isEmpty() ? null : email.trim());
				ps.executeUpdate();
				ResultSet keys = ps.getGeneratedKeys();
				if (keys.next())
					return keys.getInt(1);
			}
		} catch (SQLException e) {
			System.err.println("[StudentDAO] createStudent error: " + e.getMessage());
		}
		return -1;
	}

	public static boolean softDeleteStudent(int studentId) {
		String sql = "UPDATE students SET is_deleted = 1 WHERE student_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[StudentDAO] softDeleteStudent error: " + e.getMessage());
		}
		return false;
	}

	public static boolean updateStudent(int studentId, String fullName, String program, String yearLevel,
			String section, String contactNo, String email) {
		String sql = "UPDATE students SET full_name=?, program=?, year_level=?, " + "section=?, contact_no=?, email=? "
				+ "WHERE student_id=? AND is_deleted=0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, fullName.trim());
				ps.setString(2, program);
				ps.setString(3, yearLevel.trim().isEmpty() ? "1st Year" : yearLevel.trim());
				ps.setString(4, section.trim());
				ps.setString(5, contactNo.trim().isEmpty() ? null : contactNo.trim());
				ps.setString(6, email.trim().isEmpty() ? null : email.trim());
				ps.setInt(7, studentId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[StudentDAO] updateStudent error: " + e.getMessage());
		}
		return false;
	}

	public static List<String[]> searchStudents(String keyword, String programFilter) {
		List<String[]> list = new ArrayList<>();
		String sql = "SELECT student_id, full_name, program, year_level, section, "
				+ "       COALESCE(contact_no,'') AS contact_no, " + "       COALESCE(email,'') AS email "
				+ "FROM students " + "WHERE is_deleted = 0 AND full_name LIKE ? "
				+ (("ROTC".equals(programFilter) || "CWTS".equals(programFilter)) ? "AND program = ? " : "")
				+ "ORDER BY full_name";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, "%" + keyword.trim() + "%");
			if ("ROTC".equals(programFilter) || "CWTS".equals(programFilter))
				ps.setString(2, programFilter);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new String[] { String.valueOf(rs.getInt("student_id")), rs.getString("full_name"),
						rs.getString("program"), rs.getString("year_level"), rs.getString("section"),
						rs.getString("contact_no"), rs.getString("email") });
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.err.println("[StudentDAO] searchStudents error: " + e.getMessage());
		}
		return list;
	}

	public static int getStudentIdForUser(int userId) {
		String sql = "SELECT student_id FROM users WHERE user_id = ? AND is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return -1;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, userId);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					int sid = rs.getInt("student_id");
					return rs.wasNull() ? -1 : sid;
				}
			}
		} catch (SQLException e) {
			System.err.println("[StudentDAO] getStudentIdForUser error: " + e.getMessage());
		}
		return -1;
	}

	public static java.util.List<String[]> getCOCCStudents() {

		java.util.List<String[]> list = new java.util.ArrayList<>();

		String sql = "SELECT s.student_id, s.full_name, " + "r.rank_name, " + "s.platoon, s.battalion "
				+ "FROM students s " + "INNER JOIN cocc_ranks r " + "ON s.student_id = r.student_id "
				+ "WHERE s.program = 'ROTC'";

		try (java.sql.Connection conn = util.DBConnection.getConnection();

				java.sql.PreparedStatement ps = conn.prepareStatement(sql);

				java.sql.ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				list.add(new String[] {

						String.valueOf(rs.getInt("student_id")),

						rs.getString("full_name"),

						"",

						rs.getString("rank_name"),

						rs.getString("platoon"),

						rs.getString("battalion") });
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

}