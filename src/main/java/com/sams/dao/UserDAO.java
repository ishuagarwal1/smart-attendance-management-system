package com.sams.dao;

import com.sams.model.StudentInfo;
import com.sams.model.User;
import com.sams.util.DBUtil;
import com.sams.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data access for the users table. */
public class UserDAO {

    private static final String COLS =
        "id, username, password_hash, full_name, email, role, active, created_at, last_login";

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setLastLogin(rs.getTimestamp("last_login"));
        return u;
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLS + " FROM users WHERE username = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public User findById(long id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM users WHERE id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM users ORDER BY FIELD(role,'ADMIN','TEACHER','STUDENT'), full_name";
        List<User> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Active users of a given role (used to fill the "teacher" drop-down). */
    public List<User> findActiveByRole(String role) throws SQLException {
        String sql = "SELECT " + COLS + " FROM users WHERE role = ? AND active = 1 ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public boolean usernameExists(String username) throws SQLException {
        return exists("SELECT 1 FROM users WHERE username = ?", username);
    }

    public boolean emailExists(String email) throws SQLException {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    public boolean rollNoExists(String roll) throws SQLException {
        return exists("SELECT 1 FROM students WHERE roll_no = ?", roll);
    }

    private boolean exists(String sql, String value) throws SQLException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Creates a user (and, for students, the profile row) in ONE transaction -
     * either both rows are stored or none.
     */
    public long create(User u, String plainPassword, StudentInfo profile) throws SQLException {
        String insUser = "INSERT INTO users (username, password_hash, full_name, email, role) VALUES (?,?,?,?,?)";
        String insStud = "INSERT INTO students (user_id, roll_no, department, semester, phone) VALUES (?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(insUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, u.getUsername());
                ps.setString(2, PasswordUtil.hash(plainPassword));
                ps.setString(3, u.getFullName());
                ps.setString(4, u.getEmail());
                ps.setString(5, u.getRole());
                ps.executeUpdate();
                long id;
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); id = k.getLong(1); }
                if ("STUDENT".equals(u.getRole()) && profile != null) {
                    try (PreparedStatement ps2 = c.prepareStatement(insStud)) {
                        ps2.setLong(1, id);
                        ps2.setString(2, profile.getRollNo());
                        ps2.setString(3, profile.getDepartment());
                        ps2.setInt(4, profile.getSemester());
                        ps2.setString(5, profile.getPhone());
                        ps2.executeUpdate();
                    }
                }
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void updatePassword(long userId, String newPlainPassword) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, PasswordUtil.hash(newPlainPassword));
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public void setActive(long userId, boolean active) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET active = ? WHERE id = ?")) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public void touchLogin(long userId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET last_login = NOW() WHERE id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    /** {ADMIN=1, TEACHER=4, STUDENT=20} - used on the admin dashboard. */
    public Map<String, Integer> countByRole() throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("ADMIN", 0); m.put("TEACHER", 0); m.put("STUDENT", 0);
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT role, COUNT(*) n FROM users WHERE active = 1 GROUP BY role");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getString("role"), rs.getInt("n"));
        }
        return m;
    }
}
