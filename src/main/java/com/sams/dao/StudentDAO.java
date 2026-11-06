package com.sams.dao;

import com.sams.model.StudentInfo;
import com.sams.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access for student profiles (users JOIN students). */
public class StudentDAO {

    static final String SELECT =
        "SELECT u.id, u.username, u.full_name, u.email, u.active, s.roll_no, s.department, s.semester, s.phone "
      + "FROM students s JOIN users u ON u.id = s.user_id ";

    static StudentInfo map(ResultSet rs) throws SQLException {
        StudentInfo s = new StudentInfo();
        s.setUserId(rs.getLong("id"));
        s.setUsername(rs.getString("username"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setActive(rs.getBoolean("active"));
        s.setRollNo(rs.getString("roll_no"));
        s.setDepartment(rs.getString("department"));
        s.setSemester(rs.getInt("semester"));
        s.setPhone(rs.getString("phone"));
        return s;
    }

    public List<StudentInfo> findAll() throws SQLException {
        List<StudentInfo> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY s.roll_no");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public StudentInfo findByRollNo(String rollNo) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE s.roll_no = ?")) {
            ps.setString(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public StudentInfo findById(long userId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE u.id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
}
