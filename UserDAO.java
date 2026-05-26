package dao;

import util.DBConnection;
import util.PasswordUtil;
import java.sql.*;

public class UserDAO {

	public static String[] validateLoginFull(String username, String plainPassword) {
		String sql = "SELECT u.user_id, u.username, u.role, u.student_id, " + "       s.full_name, s.program "
				+ "FROM users u " + "LEFT JOIN students s ON u.student_id = s.student_id " + "WHERE u.username = ? "
				+ "  AND u.password_hash = ? " + "  AND u.is_deleted = 0 " + "  AND u.is_active = 1";

		String hash = PasswordUtil.hashPassword(plainPassword);

		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return null;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, username.trim());
				ps.setString(2, hash);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					return new String[] { String.valueOf(rs.getInt("user_id")), rs.getString("username"),
							rs.getString("role"), rs.getString("student_id"), rs.getString("full_name"),
							rs.getString("program") };
				}
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] validateLoginFull error: " + e.getMessage());
		}
		return null;
	}

	public static String[] getUserById(int userId) {
		String sql = "SELECT u.user_id, u.username, u.role, u.student_id, "
				+ "       s.full_name, s.program, u.last_login " + "FROM users u "
				+ "LEFT JOIN students s ON u.student_id = s.student_id " + "WHERE u.user_id = ? AND u.is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return null;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, userId);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					return new String[] { String.valueOf(rs.getInt("user_id")), rs.getString("username"),
							rs.getString("role"), rs.getString("student_id"),
							rs.getString("full_name") != null ? rs.getString("full_name") : "—",
							rs.getString("program") != null ? rs.getString("program") : "—",
							rs.getString("last_login") != null ? rs.getString("last_login") : "Never" };
				}
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] getUserById error: " + e.getMessage());
		}
		return null;
	}

	public static boolean updateUsername(int userId, String newUsername) {
		if (usernameExists(newUsername))
			return false;
		String sql = "UPDATE users SET username = ? WHERE user_id = ? AND is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, newUsername.trim());
				ps.setInt(2, userId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] updateUsername error: " + e.getMessage());
		}
		return false;
	}

	public static java.util.List<String[]> getUsersByRole(String role) {
		java.util.List<String[]> list = new java.util.ArrayList<>();
		String sql = "SELECT u.user_id, u.username, u.student_id, s.full_name " + "FROM users u "
				+ "LEFT JOIN students s ON u.student_id = s.student_id "
				+ "WHERE u.role = ? AND u.is_deleted = 0 AND u.is_active = 1 " + "ORDER BY u.username";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, role);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("user_id")), rs.getString("username"),
							rs.getString("student_id"),
							rs.getString("full_name") != null ? rs.getString("full_name") : "—" });
				}
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] getUsersByRole error: " + e.getMessage());
		}
		return list;
	}

	public static void recordLoginTimestamp(int userId) {
		String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, userId);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] recordLoginTimestamp error: " + e.getMessage());
		}
	}

	public static boolean createUser(String username, String plainPassword, String role, Integer studentId) {
		String sql = "INSERT INTO users (username, password_hash, role, student_id, is_active, is_deleted) "
				+ "VALUES (?, ?, ?, ?, 1, 0)";
		String hash = PasswordUtil.hashPassword(plainPassword);
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, username.trim());
				ps.setString(2, hash);
				ps.setString(3, role);
				if (studentId != null)
					ps.setInt(4, studentId);
				else
					ps.setNull(4, Types.INTEGER);
				ps.executeUpdate();
				return true;
			}
		} catch (SQLIntegrityConstraintViolationException e) {
			System.err.println("[UserDAO] createUser duplicate username: " + username);
		} catch (SQLException e) {
			System.err.println("[UserDAO] createUser error: " + e.getMessage());
		}
		return false;
	}

	public static boolean softDeleteUser(int userId) {
		String sql = "UPDATE users SET is_deleted = 1, is_active = 0 WHERE user_id = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, userId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] softDeleteUser error: " + e.getMessage());
		}
		return false;
	}

	public static boolean updateUserRole(int userId, String newRole) {
		String sql = "UPDATE users SET role = ? WHERE user_id = ? AND is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, newRole);
				ps.setInt(2, userId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] updateUserRole error: " + e.getMessage());
		}
		return false;
	}

	public static boolean updatePassword(int userId, String oldPlain, String newPlain) {

		String checkSql = "SELECT user_id FROM users WHERE user_id = ? AND password_hash = ? AND is_deleted = 0";
		String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
		String oldHash = PasswordUtil.hashPassword(oldPlain);
		String newHash = PasswordUtil.hashPassword(newPlain);
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement check = conn.prepareStatement(checkSql)) {
				check.setInt(1, userId);
				check.setString(2, oldHash);
				ResultSet rs = check.executeQuery();
				if (!rs.next())
					return false;
			}
			try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
				upd.setString(1, newHash);
				upd.setInt(2, userId);
				return upd.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] updatePassword error: " + e.getMessage());
		}
		return false;
	}

	public static boolean usernameExists(String username) {
		String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, username.trim());
				ResultSet rs = ps.executeQuery();
				return rs.next() && rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] usernameExists error: " + e.getMessage());
		}
		return false;
	}

	public static int getUserIdByStudentId(int studentId) {
		String sql = "SELECT user_id FROM users WHERE student_id = ? AND is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return -1;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					return rs.getInt("user_id");
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] getUserIdByStudentId error: " + e.getMessage());
		}
		return -1;
	}

	public static int getUserId(String username) {
		String sql = "SELECT user_id FROM users WHERE username = ? AND is_deleted = 0";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return -1;
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, username.trim());
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					return rs.getInt("user_id");
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] getUserId error: " + e.getMessage());
		}
		return -1;
	}

	public static java.util.List<String[]> getAllUsers() {
		java.util.List<String[]> list = new java.util.ArrayList<>();
		String sql = "SELECT u.user_id, u.username, u.role, u.student_id, "
				+ "       s.full_name, s.program, u.last_login " + "FROM users u "
				+ "LEFT JOIN students s ON u.student_id = s.student_id " + "WHERE u.is_deleted = 0 "
				+ "ORDER BY u.role, u.username";
		try (Connection conn = DBConnection.getConnection()) {
			if (conn == null)
				return list;
			try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(new String[] { String.valueOf(rs.getInt("user_id")), rs.getString("username"),
							rs.getString("role"), rs.getString("student_id"),
							rs.getString("full_name") != null ? rs.getString("full_name") : "—",
							rs.getString("program") != null ? rs.getString("program") : "—",
							rs.getString("last_login") != null ? rs.getString("last_login") : "Never" });
				}
			}
		} catch (SQLException e) {
			System.err.println("[UserDAO] getAllUsers error: " + e.getMessage());
		}
		return list;
	}
}